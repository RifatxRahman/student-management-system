package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.InstitutionProperties;
import com.sms.studentmanagement.dto.CampaignDtos.CampaignResponse;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateRequest;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateResponse;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderRequest;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderResponse;
import com.sms.studentmanagement.dto.CampaignDtos.SendCampaignRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.NotificationAudience;
import com.sms.studentmanagement.entity.NotificationCampaign;
import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.entity.NotificationTemplate;
import com.sms.studentmanagement.entity.NotificationType;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.InvoiceRepository;
import com.sms.studentmanagement.repository.NotificationCampaignRepository;
import com.sms.studentmanagement.repository.NotificationLogRepository;
import com.sms.studentmanagement.repository.NotificationTemplateRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.util.SmsCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCampaignService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCampaignService.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationCampaignRepository campaignRepository;
    private final NotificationLogRepository logRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SmsSender smsSender;
    private final AuditService auditService;
    private final InstitutionProperties institutionProperties;

    // ==================== 1. Template Management ====================

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplates(NotificationType category, Boolean activeOnly) {
        List<NotificationTemplate> list;
        if (category != null) {
            list = templateRepository.findByCategoryAndActiveTrue(category);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            list = templateRepository.findByActiveTrueOrderByNameAsc();
        } else {
            list = templateRepository.findAllByOrderByNameAsc();
        }

        List<NotificationTemplateResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(NotificationTemplateResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplate(Long id) {
        NotificationTemplate template = findTemplate(id);
        return NotificationTemplateResponse.from(template);
    }

    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (templateRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের টেমপ্লেট ইতিমধ্যে বিদ্যমান: " + name);
        }
        if (templateRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের টেমপ্লেট ইতিমধ্যে বিদ্যমান: " + code);
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setName(name);
        template.setCode(code);
        template.setChannel(request.channel());
        template.setCategory(request.category());
        template.setSubject(request.subject() != null ? request.subject().trim() : null);
        template.setTemplateBody(request.templateBody().trim());
        template.setVariables(request.variables() != null ? request.variables().trim() : null);
        template.setActive(request.active() == null || request.active());

        NotificationTemplate saved = templateRepository.save(template);
        auditService.log("NOTIFICATION_TEMPLATE_CREATED", "NOTIFICATION_TEMPLATE", saved.getId(),
                saved.getName() + " (" + saved.getCode() + ")");
        return NotificationTemplateResponse.from(saved);
    }

    @Transactional
    public NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request) {
        NotificationTemplate template = findTemplate(id);
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (templateRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের টেমপ্লেট অন্য রেকর্ডে বিদ্যমান: " + name);
        }
        if (templateRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের টেমপ্লেট অন্য রেকর্ডে বিদ্যমান: " + code);
        }

        template.setName(name);
        template.setCode(code);
        template.setChannel(request.channel());
        template.setCategory(request.category());
        template.setSubject(request.subject() != null ? request.subject().trim() : null);
        template.setTemplateBody(request.templateBody().trim());
        template.setVariables(request.variables() != null ? request.variables().trim() : null);
        if (request.active() != null) {
            template.setActive(request.active());
        }

        NotificationTemplate updated = templateRepository.save(template);
        auditService.log("NOTIFICATION_TEMPLATE_UPDATED", "NOTIFICATION_TEMPLATE", updated.getId(),
                updated.getName() + " (" + updated.getCode() + ")");
        return NotificationTemplateResponse.from(updated);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        NotificationTemplate template = findTemplate(id);
        templateRepository.delete(template);
        auditService.log("NOTIFICATION_TEMPLATE_DELETED", "NOTIFICATION_TEMPLATE", id, template.getName());
    }

    // ==================== 2. Placeholder Preview ====================

    public PreviewPlaceholderResponse previewPlaceholder(PreviewPlaceholderRequest request) {
        String body = request.templateBody();
        String rendered = body
                .replace("{student_name}", "সাকিব আল হাসান")
                .replace("{class_name}", "১০ম শ্রেণি")
                .replace("{roll_no}", "০১")
                .replace("{due_amount}", "১,৫০০")
                .replace("{exam_name}", "বার্ষিক মূল্যায়ন পরীক্ষা")
                .replace("{date}", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("{start_date}", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("{end_date}", LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("{school_name}", institutionProperties.getName())
                .replace("{message}", "আগামীকাল বিদ্যালয় যথারীতি খোলা থাকবে");

        boolean unicode = SmsCalculator.isUnicode(rendered);
        int parts = SmsCalculator.calculateParts(rendered);

        return new PreviewPlaceholderResponse(
                body,
                rendered,
                rendered.length(),
                parts,
                unicode ? "BN (বাংলা ইউনিকোড)" : "EN (ইংরেজি GSM)"
        );
    }

    // ==================== 3. Campaign Broadcast & Dispatch ====================

    @Transactional
    public CampaignResponse sendCampaign(SendCampaignRequest request, String currentUsername) {
        AcademicSession session = sessionRepository.findByActiveTrue()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "কোনো সক্রিয় শিক্ষাবর্ষ নেই।"));

        SchoolClass targetClass = null;
        if (request.targetClassId() != null) {
            targetClass = classRepository.findById(request.targetClassId())
                    .orElseThrow(() -> new NotFoundException("শ্রেণি পাওয়া যায়নি: " + request.targetClassId()));
        }

        Section targetSection = null;
        if (request.targetSectionId() != null) {
            targetSection = sectionRepository.findById(request.targetSectionId())
                    .orElseThrow(() -> new NotFoundException("সেকশন পাওয়া যায়নি: " + request.targetSectionId()));
        }

        List<CampaignRecipient> recipients = resolveRecipients(
                request.targetAudience(), session.getId(), targetClass, targetSection);

        if (recipients.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "নির্বাচিত ক্যাটাগরির জন্য কোনো প্রাপক শিক্ষার্থী পাওয়া যায়নি।");
        }

        NotificationCampaign campaign = new NotificationCampaign();
        campaign.setTitle(request.title().trim());
        campaign.setChannel(request.channel());
        campaign.setTargetAudience(request.targetAudience());
        campaign.setTargetClass(targetClass);
        campaign.setTargetSection(targetSection);
        campaign.setMessage(request.message().trim());
        campaign.setTotalRecipients(recipients.size());
        campaign.setCreatedBy(currentUsername != null ? currentUsername : "ADMIN");

        NotificationCampaign savedCampaign = campaignRepository.save(campaign);

        int sentCount = 0;
        int failedCount = 0;
        LocalDate today = LocalDate.now();
        String schoolName = institutionProperties.getName();

        for (int i = 0; i < recipients.size(); i++) {
            CampaignRecipient r = recipients.get(i);
            String personalizedMessage = renderPersonalizedMessage(
                    request.message(), r, schoolName, today);

            int parts = SmsCalculator.calculateParts(personalizedMessage);
            String lang = SmsCalculator.isUnicode(personalizedMessage) ? "BN" : "EN";

            NotificationLog logEntry = new NotificationLog();
            logEntry.setStudent(r.student());
            logEntry.setGuardian(r.guardian());
            logEntry.setNotificationType(resolveNotificationType(request.targetAudience()));
            logEntry.setEventDate(today);
            logEntry.setPhone(r.phone());
            logEntry.setMessage(personalizedMessage);
            logEntry.setLanguage(lang);
            logEntry.setSmsParts(parts);
            logEntry.setAttempts(1);

            try {
                smsSender.send(r.phone(), personalizedMessage);
                logEntry.setStatus(NotificationStatus.SENT);
                logEntry.setSentAt(java.time.Instant.now());
                sentCount++;
            } catch (Exception ex) {
                log.warn("SMS send failed to {}: {}", r.phone(), ex.getMessage());
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setLastError(ex.getMessage() != null && ex.getMessage().length() > 250
                        ? ex.getMessage().substring(0, 250) : ex.getMessage());
                failedCount++;
            }

            logRepository.save(logEntry);
        }

        savedCampaign.setSentCount(sentCount);
        savedCampaign.setFailedCount(failedCount);
        campaignRepository.save(savedCampaign);

        auditService.log("NOTIFICATION_CAMPAIGN_SENT", "NOTIFICATION_CAMPAIGN", savedCampaign.getId(),
                savedCampaign.getTitle() + " | প্রাপক: " + savedCampaign.getTotalRecipients()
                        + " জন | সফল: " + sentCount + " | ব্যর্থ: " + failedCount);

        return CampaignResponse.from(savedCampaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns() {
        List<NotificationCampaign> list = campaignRepository.findAllWithDetails();
        List<CampaignResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(CampaignResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long id) {
        NotificationCampaign campaign = campaignRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("ক্যাম্পেইন পাওয়া যায়নি: " + id));
        return CampaignResponse.from(campaign);
    }

    // ==================== Helper Methods ====================

    private List<CampaignRecipient> resolveRecipients(
            NotificationAudience audience, Long sessionId, SchoolClass targetClass, Section targetSection) {

        List<CampaignRecipient> result = new ArrayList<>();

        if (audience == NotificationAudience.FEE_DEFAULTERS) {
            // বকেয়া ফি থাকা শিক্ষার্থীদের ইনভয়েস
            List<Invoice> unpaidInvoices = invoiceRepository.searchInvoices(
                    sessionId, null, null, null, null, null);

            Map<Long, BigDecimal> studentDues = new HashMap<>();
            Map<Long, Invoice> sampleInvoicePerStudent = new HashMap<>();

            for (int i = 0; i < unpaidInvoices.size(); i++) {
                Invoice inv = unpaidInvoices.get(i);
                if (inv.getStatus() != InvoiceStatus.CANCELLED && inv.getDueAmount().compareTo(BigDecimal.ZERO) > 0) {
                    Long sId = inv.getStudent().getId();
                    BigDecimal currentDue = studentDues.getOrDefault(sId, BigDecimal.ZERO);
                    studentDues.put(sId, currentDue.add(inv.getDueAmount()));
                    sampleInvoicePerStudent.putIfAbsent(sId, inv);
                }
            }

            List<Long> defaulterIds = new ArrayList<>(studentDues.keySet());
            for (int i = 0; i < defaulterIds.size(); i++) {
                Long studentId = defaulterIds.get(i);
                Invoice inv = sampleInvoicePerStudent.get(studentId);
                Student s = inv.getStudent();
                Guardian g = resolveGuardian(s);
                String phone = (g != null && g.getPhone() != null) ? g.getPhone() : s.getPhone();

                if (phone != null && !phone.isBlank()) {
                    result.add(new CampaignRecipient(
                            s, g, phone, inv.getSchoolClass().getName(),
                            inv.getSection() != null ? inv.getSection().getName() : "",
                            1, studentDues.get(studentId)
                    ));
                }
            }
            return result;
        }

        // সাধারণ শিক্ষার্থী তালিকা
        List<Enrollment> enrollments;
        if (audience == NotificationAudience.SECTION && targetSection != null) {
            enrollments = enrollmentRepository.findInSection(
                    targetSection.getId(), LocalDate.now(), LocalDate.now());
        } else if (audience == NotificationAudience.CLASS && targetClass != null) {
            enrollments = enrollmentRepository.findInClassAndSession(
                    sessionId, targetClass.getId(), EnrollmentStatus.ACTIVE);
        } else {
            // ALL active
            enrollments = enrollmentRepository.findAll();
            List<Enrollment> activeOnly = new ArrayList<>();
            for (int i = 0; i < enrollments.size(); i++) {
                Enrollment e = enrollments.get(i);
                if (e.getStatus() == EnrollmentStatus.ACTIVE && e.getSession().getId().equals(sessionId)) {
                    activeOnly.add(e);
                }
            }
            enrollments = activeOnly;
        }

        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment e = enrollments.get(i);
            Student s = e.getStudent();
            Guardian g = resolveGuardian(s);
            String phone = (g != null && g.getPhone() != null) ? g.getPhone() : s.getPhone();

            if (phone != null && !phone.isBlank()) {
                result.add(new CampaignRecipient(
                        s, g, phone, e.getSchoolClass().getName(),
                        e.getSection().getName(), e.getRollNumber(), BigDecimal.ZERO
                ));
            }
        }

        return result;
    }

    private Guardian resolveGuardian(Student s) {
        List<StudentGuardian> links = studentGuardianRepository.findAllWithGuardian(s.getId());
        if (!links.isEmpty()) {
            return links.get(0).getGuardian();
        }
        Guardian dummy = new Guardian();
        dummy.setId(0L);
        dummy.setFullName(s.getFullName() + "-এর অভিভাবক");
        dummy.setPhone(s.getPhone() != null ? s.getPhone() : "01700000000");
        return dummy;
    }

    private String renderPersonalizedMessage(
            String template, CampaignRecipient r, String schoolName, LocalDate date) {
        return template
                .replace("{student_name}", r.student().getFullName())
                .replace("{class_name}", r.className())
                .replace("{roll_no}", String.valueOf(r.rollNumber()))
                .replace("{due_amount}", r.dueAmount().toPlainString())
                .replace("{date}", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("{school_name}", schoolName);
    }

    private NotificationType resolveNotificationType(NotificationAudience audience) {
        if (audience == NotificationAudience.FEE_DEFAULTERS) {
            return NotificationType.FEE_DUE;
        }
        return NotificationType.ANNOUNCEMENT;
    }

    private NotificationTemplate findTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("নোটিফিকেশন টেমপ্লেট পাওয়া যায়নি: " + id));
    }

    record CampaignRecipient(
            Student student,
            Guardian guardian,
            String phone,
            String className,
            String sectionName,
            int rollNumber,
            BigDecimal dueAmount
    ) {}
}
