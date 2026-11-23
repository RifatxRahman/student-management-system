package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.InstitutionProperties;
import com.sms.studentmanagement.config.SmsProperties;
import com.sms.studentmanagement.dto.NotificationDtos.DispatchResult;
import com.sms.studentmanagement.dto.NotificationDtos.NotificationResponse;
import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.entity.FeePayment;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.entity.NotificationType;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.NotificationLogRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.util.SmsCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int DISPATCH_BATCH = 50;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final NotificationLogRepository repository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SmsSender smsSender;
    private final AuditService auditService;
    private final SmsProperties smsProperties;
    private final InstitutionProperties institutionProperties;

    // ---------- জমা করা (হাজিরা থেকে ডাকা হয়) ----------

    /** একজন শিক্ষার্থীর একদিনে একটাই বার্তা। প্রধান অভিভাবক না থাকলে তালিকার প্রথমজন। */
    @Transactional
    public void queueAbsence(Student student, LocalDate date) {
        if (repository.existsByStudentIdAndNotificationTypeAndEventDate(
                student.getId(), NotificationType.ABSENCE, date)) {
            return;
        }
        List<StudentGuardian> links = studentGuardianRepository.findAllWithGuardian(student.getId());
        if (links.isEmpty()) {
            return;
        }
        Guardian guardian = links.get(0).getGuardian();

        String lang = smsProperties.getDefaultLanguage();
        String schoolName = institutionProperties.getName();
        String message = buildAbsenceMessage(schoolName, student, date, lang);
        int parts = SmsCalculator.calculateParts(message);

        NotificationLog logEntry = new NotificationLog();
        logEntry.setStudent(student);
        logEntry.setGuardian(guardian);
        logEntry.setNotificationType(NotificationType.ABSENCE);
        logEntry.setEventDate(date);
        logEntry.setPhone(guardian.getPhone());
        logEntry.setMessage(message);
        logEntry.setLanguage(lang);
        logEntry.setSmsParts(parts);
        logEntry.setStatus(NotificationStatus.PENDING);
        logEntry.setAttempts(0);
        repository.save(logEntry);
    }

    private String buildAbsenceMessage(String schoolName, Student student, LocalDate date, String lang) {
        if ("EN".equalsIgnoreCase(lang)) {
            return String.format("[%s] Dear Guardian, your child %s (%s) was absent on %s.",
                    schoolName, student.getFullName(), student.getStudentCode(), date);
        } else {
            return String.format("[%s] সম্মানিত অভিভাবক, আপনার সন্তান %s (%s) আজ %s তারিখে বিদ্যালয়ে অনুপস্থিত ছিল।",
                    schoolName, student.getFullName(), student.getStudentCode(), date);
        }
    }

    /** ABSENT ভুল ছিল: যে বার্তা এখনো যায়নি (PENDING/FAILED) সেটা মুছে ফেলা। SENT থাকে। */
    @Transactional
    public void cancelUnsentAbsence(Long studentId, LocalDate date) {
        repository.deleteByStudentIdAndNotificationTypeAndEventDateAndStatusIn(
                studentId, NotificationType.ABSENCE, date,
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED));
    }

    /** ফি পেমেন্ট সফল হলে অভিভাবকের ফোনে তাৎক্ষণিক স্বয়ংক্রিয় রসিদ এসএমএস প্রেরণ। */
    @Transactional
    public void sendPaymentReceiptSms(Student student, FeePayment payment, BigDecimal remainingDue) {
        if (student == null || payment == null) {
            return;
        }
        List<StudentGuardian> links = studentGuardianRepository.findAllWithGuardian(student.getId());
        Guardian guardian = links.isEmpty() ? null : links.get(0).getGuardian();
        String phone = (guardian != null && guardian.getPhone() != null && !guardian.getPhone().isBlank())
                ? guardian.getPhone() : student.getPhone();

        if (phone == null || phone.isBlank()) {
            log.info("No phone number found for student {}, skipping payment SMS", student.getId());
            return;
        }

        String schoolName = institutionProperties.getName();
        String className = payment.getInvoice() != null && payment.getInvoice().getSchoolClass() != null
                ? payment.getInvoice().getSchoolClass().getName() : "";
        String cleanAmount = payment.getAmount() != null
                ? payment.getAmount().stripTrailingZeros().toPlainString() : "0";
        String cleanDue = remainingDue != null
                ? remainingDue.stripTrailingZeros().toPlainString() : "0";

        String message = String.format(
                "[%s] প্রিয় অভিভাবক, আপনার সন্তান %s (%s)-এর ৳%s ফি সফলভাবে গ্রহণ করা হয়েছে। রসিদ নং: %s। অবশিষ্ট বকেয়া: ৳%s। ধন্যবাদ।",
                schoolName,
                student.getFullName(),
                className,
                cleanAmount,
                payment.getReceiptNumber(),
                cleanDue
        );

        int parts = SmsCalculator.calculateParts(message);
        String lang = SmsCalculator.isUnicode(message) ? "BN" : "EN";

        NotificationLog logEntry = new NotificationLog();
        logEntry.setStudent(student);
        logEntry.setGuardian(guardian);
        logEntry.setNotificationType(NotificationType.PAYMENT_RECEIPT);
        logEntry.setEventDate(LocalDate.now());
        logEntry.setPhone(phone);
        logEntry.setMessage(message);
        logEntry.setLanguage(lang);
        logEntry.setSmsParts(parts);
        logEntry.setAttempts(1);

        try {
            smsSender.send(phone, message);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setSentAt(Instant.now());
        } catch (Exception ex) {
            log.warn("Payment receipt SMS send failed to {}: {}", phone, ex.getMessage());
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setLastError(ex.getMessage() != null && ex.getMessage().length() > 250
                    ? ex.getMessage().substring(0, 250) : ex.getMessage());
        }

        repository.save(logEntry);
    }

    // ---------- পাঠানো ----------

    /**
     * PENDING বার্তাগুলো (একবারে সর্বোচ্চ ৫০টা) পাঠানোর চেষ্টা। প্রতিটা বার্তা আলাদাভাবে সেভ হয়,
     * তাই মাঝপথে থেমে গেলেও আগেরগুলোর অবস্থা ঠিক থাকে।
     */
    public DispatchResult dispatchPending() {
        List<NotificationLog> batch = repository.findByStatusOrderByIdAsc(
                NotificationStatus.PENDING, PageRequest.of(0, DISPATCH_BATCH));

        int sent = 0;
        int failed = 0;
        for (int i = 0; i < batch.size(); i++) {
            NotificationLog n = batch.get(i);
            try {
                smsSender.send(n.getPhone(), n.getMessage());
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(Instant.now());
                n.setLastError(null);
                sent++;
            } catch (RuntimeException e) {
                n.setStatus(NotificationStatus.FAILED);
                n.setLastError(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 250));
                failed++;
            }
            n.setAttempts(n.getAttempts() + 1);
            repository.save(n);
        }

        boolean demo = smsSender.isDemo();
        if (sent + failed > 0) {
            auditService.log("NOTIFICATION_DISPATCHED", "NOTIFICATION", null,
                    "পাঠানো " + sent + ", ব্যর্থ " + failed + (demo ? " (ডেমো মোড)" : ""));
        }
        String note = demo
                ? "ডেমো মোড: আসল SMS পাঠানো হয়নি (কনসোলে লগ হয়েছে)।"
                : "পাঠানোর চেষ্টা শেষ।";
        return new DispatchResult(sent, failed, demo, note);
    }

    // ---------- স্বয়ংক্রিয় শিডিউলার (Cron Job) ----------
    @Scheduled(cron = "0 15,30,45 11-14 * * SUN-THU", zone = "Asia/Dhaka")
    public void scheduledAutoDispatch() {
        if (!smsProperties.isSchedulerEnabled()) {
            return;
        }
        log.info("[SCHEDULED] Auto SMS dispatch job started.");

        DispatchResult result = dispatchPending();
        if (result.sent() > 0 || result.failed() > 0) {
            log.info("[SCHEDULED] Auto SMS dispatch: Sent={}, Failed={}", result.sent(), result.failed());
        }

        List<NotificationLog> failedList = repository.findRetryableFailed(
                NotificationStatus.FAILED, MAX_RETRY_ATTEMPTS, PageRequest.of(0, 20));

        for (int i = 0; i < failedList.size(); i++) {
            NotificationLog item = failedList.get(i);
            item.setStatus(NotificationStatus.PENDING);
            repository.save(item);
        }
    }

    @Transactional
    public NotificationResponse retry(Long id) {
        NotificationLog n = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("বার্তা পাওয়া যায়নি"));
        if (n.getStatus() != NotificationStatus.FAILED) {
            throw new ApiException(HttpStatus.CONFLICT, "শুধু ব্যর্থ (FAILED) বার্তাই আবার চেষ্টা করা যায়");
        }
        n.setStatus(NotificationStatus.PENDING);
        auditService.log("NOTIFICATION_RETRY", "NOTIFICATION", id, "আবার পাঠানোর জন্য PENDING করা হলো");
        return NotificationResponse.from(n);
    }

    // ---------- তালিকা ----------
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(NotificationStatus status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<NotificationLog> result = repository.search(status, PageRequest.of(Math.max(page, 0), safeSize));
        return PageResponse.of(result, result.stream().map(NotificationResponse::from).toList());
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}