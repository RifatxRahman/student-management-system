package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.config.InstitutionProperties;
import com.sms.studentmanagement.dto.CampaignDtos.CampaignResponse;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateRequest;
import com.sms.studentmanagement.dto.CampaignDtos.NotificationTemplateResponse;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderRequest;
import com.sms.studentmanagement.dto.CampaignDtos.PreviewPlaceholderResponse;
import com.sms.studentmanagement.dto.CampaignDtos.SendCampaignRequest;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.NotificationAudience;
import com.sms.studentmanagement.entity.NotificationCampaign;
import com.sms.studentmanagement.entity.NotificationChannel;
import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationTemplate;
import com.sms.studentmanagement.entity.NotificationType;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.InvoiceRepository;
import com.sms.studentmanagement.repository.NotificationCampaignRepository;
import com.sms.studentmanagement.repository.NotificationLogRepository;
import com.sms.studentmanagement.repository.NotificationTemplateRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NotificationCampaignServiceTest {

    @Mock
    NotificationTemplateRepository templateRepository;

    @Mock
    NotificationCampaignRepository campaignRepository;

    @Mock
    NotificationLogRepository logRepository;

    @Mock
    StudentGuardianRepository studentGuardianRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SchoolClassRepository classRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    SmsSender smsSender;

    @Mock
    AuditService auditService;

    InstitutionProperties institutionProperties;
    NotificationCampaignService service;

    @BeforeEach
    void setUp() {
        institutionProperties = new InstitutionProperties();
        institutionProperties.setName("অগ্রণী উচ্চ বিদ্যালয়");

        service = new NotificationCampaignService(
                templateRepository,
                campaignRepository,
                logRepository,
                studentGuardianRepository,
                enrollmentRepository,
                sessionRepository,
                classRepository,
                sectionRepository,
                invoiceRepository,
                smsSender,
                auditService,
                institutionProperties
        );
    }

    @Test
    void createTemplate_success() {
        NotificationTemplateRequest req = new NotificationTemplateRequest(
                "বকেয়া ফি তাগাদা",
                "FEE_DUE_BN",
                NotificationChannel.SMS,
                NotificationType.FEE_DUE,
                null,
                "প্রিয় {student_name}, আপনার বকেয়া ৳{due_amount}।",
                "student_name,due_amount",
                true
        );

        when(templateRepository.existsByNameIgnoreCase("বকেয়া ফি তাগাদা")).thenReturn(false);
        when(templateRepository.existsByCodeIgnoreCase("FEE_DUE_BN")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(i -> {
            NotificationTemplate t = i.getArgument(0);
            t.setId(1L);
            return t;
        });

        NotificationTemplateResponse res = service.createTemplate(req);
        assertNotNull(res);
        assertEquals(1L, res.id());
        assertEquals("FEE_DUE_BN", res.code());
        assertEquals(NotificationType.FEE_DUE, res.category());
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void createTemplate_duplicateCode_throwsConflict() {
        NotificationTemplateRequest req = new NotificationTemplateRequest(
                "বকেয়া ফি তাগাদা", "FEE_DUE_BN", NotificationChannel.SMS,
                NotificationType.FEE_DUE, null, "টেক্সট", null, true
        );
        when(templateRepository.existsByNameIgnoreCase("বকেয়া ফি তাগাদা")).thenReturn(false);
        when(templateRepository.existsByCodeIgnoreCase("FEE_DUE_BN")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.createTemplate(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void previewPlaceholder_rendersAllVariablesCorrectly() {
        PreviewPlaceholderRequest req = new PreviewPlaceholderRequest(
                "প্রিয় অভিভাবক, {student_name} ({class_name})-এর বকেয়া ৳{due_amount}। - {school_name}", null
        );

        PreviewPlaceholderResponse res = service.previewPlaceholder(req);
        assertNotNull(res);
        assertTrue(res.renderedMessage().contains("সাকিব আল হাসান"));
        assertTrue(res.renderedMessage().contains("১০ম শ্রেণি"));
        assertTrue(res.renderedMessage().contains("১,৫০০"));
        assertTrue(res.renderedMessage().contains("অগ্রণী উচ্চ বিদ্যালয়"));
        assertTrue(res.characterCount() > 0);
        assertTrue(res.smsParts() >= 1);
        assertTrue(res.language().contains("BN"));
    }

    @Test
    void sendCampaign_toFeeDefaulters_dispatchesPersonalizedSms() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setName("Class 9");

        Section sec = new Section();
        sec.setName("A");

        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("STU-10");
        student.setFullName("মুরাদ হোসেন");
        student.setPhone("01711223344");

        Guardian guardian = new Guardian();
        guardian.setId(20L);
        guardian.setFullName("বুলবুল আহমেদ");
        guardian.setPhone("01811223344");

        StudentGuardian sg = new StudentGuardian();
        sg.setStudent(student);
        sg.setGuardian(guardian);
        sg.setPrimaryContact(true);

        List<StudentGuardian> guardianLinks = new ArrayList<>();
        guardianLinks.add(sg);

        Invoice inv = new Invoice();
        inv.setId(100L);
        inv.setStudent(student);
        inv.setSchoolClass(sc);
        inv.setSection(sec);
        inv.setDueAmount(new BigDecimal("2500.00"));
        inv.setStatus(InvoiceStatus.UNPAID);

        List<Invoice> invoiceList = new ArrayList<>();
        invoiceList.add(inv);

        when(sessionRepository.findByActiveTrue()).thenReturn(Optional.of(session));
        when(invoiceRepository.searchInvoices(1L, null, null, null, null, null)).thenReturn(invoiceList);
        when(studentGuardianRepository.findAllWithGuardian(10L)).thenReturn(guardianLinks);

        when(campaignRepository.save(any(NotificationCampaign.class))).thenAnswer(i -> {
            NotificationCampaign c = i.getArgument(0);
            if (c.getId() == null) c.setId(5L);
            return c;
        });

        SendCampaignRequest req = new SendCampaignRequest(
                "বকেয়া ফি তাগাদা ক্যাম্পেইন",
                NotificationChannel.SMS,
                NotificationAudience.FEE_DEFAULTERS,
                null,
                null,
                null,
                "প্রিয় অভিভাবক, {student_name}-এর ৳{due_amount} বকেয়া আছে।"
        );

        CampaignResponse res = service.sendCampaign(req, "accountant");

        assertNotNull(res);
        assertEquals(5L, res.id());
        assertEquals(1, res.totalRecipients());
        assertEquals(1, res.sentCount());
        assertEquals(0, res.failedCount());

        verify(smsSender).send(eq("01811223344"), eq("প্রিয় অভিভাবক, মুরাদ হোসেন-এর ৳2500.00 বকেয়া আছে।"));
        verify(logRepository).save(any(NotificationLog.class));
        verify(auditService).log(any(), any(), any(), any());
    }
}
