package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.NotificationDtos.DispatchResult;
import com.sms.studentmanagement.dto.NotificationDtos.NotificationResponse;
import com.sms.studentmanagement.dto.PageResponse;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DISPATCH_BATCH = 50;

    private final NotificationLogRepository repository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SmsSender smsSender;
    private final AuditService auditService;

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

        NotificationLog log = new NotificationLog();
        log.setStudent(student);
        log.setGuardian(guardian);
        log.setNotificationType(NotificationType.ABSENCE);
        log.setEventDate(date);
        log.setPhone(guardian.getPhone());
        log.setMessage("আপনার সন্তান " + student.getFullName() + " (" + student.getStudentCode() + ") "
                + date + " তারিখে অনুপস্থিত ছিল।");
        log.setStatus(NotificationStatus.PENDING);
        log.setAttempts(0);
        repository.save(log);
    }

    /** ABSENT ভুল ছিল: যে বার্তা এখনো যায়নি (PENDING/FAILED) সেটা মুছে ফেলা। SENT থাকে। */
    @Transactional
    public void cancelUnsentAbsence(Long studentId, LocalDate date) {
        repository.deleteByStudentIdAndNotificationTypeAndEventDateAndStatusIn(
                studentId, NotificationType.ABSENCE, date,
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED));
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
        for (NotificationLog n : batch) {
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
                ? "ডেমো মোড: আসল SMS পাঠানো হয়নি। আসল Gateway Phase 5-এ যুক্ত হবে।"
                : "পাঠানোর চেষ্টা শেষ।";
        return new DispatchResult(sent, failed, demo, note);
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