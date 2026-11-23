package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.FeeCollectionDtos.ApplyFineRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.InvoiceResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentCollectionRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentReceiptResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.FeeFrequency;
import com.sms.studentmanagement.entity.FeeHead;
import com.sms.studentmanagement.entity.FeePayment;
import com.sms.studentmanagement.entity.FeeStructure;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.PaymentMethod;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentWaiver;
import com.sms.studentmanagement.entity.WaiverType;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.FeeHeadRepository;
import com.sms.studentmanagement.repository.FeePaymentRepository;
import com.sms.studentmanagement.repository.FeeStructureRepository;
import com.sms.studentmanagement.repository.InvoiceItemRepository;
import com.sms.studentmanagement.repository.InvoiceRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.repository.StudentWaiverRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class FeeCollectionServiceTest {

    @Mock
    StudentWaiverRepository waiverRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    InvoiceItemRepository invoiceItemRepository;

    @Mock
    FeePaymentRepository paymentRepository;

    @Mock
    FeeHeadRepository feeHeadRepository;

    @Mock
    FeeStructureRepository feeStructureRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SchoolClassRepository classRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    AuditService auditService;

    FeeCollectionService service;

    @BeforeEach
    void setUp() {
        service = new FeeCollectionService(
                waiverRepository,
                invoiceRepository,
                invoiceItemRepository,
                paymentRepository,
                feeHeadRepository,
                feeStructureRepository,
                studentRepository,
                sessionRepository,
                classRepository,
                sectionRepository,
                enrollmentRepository,
                auditService
        );
    }

    @Test
    void generateBulkInvoices_withWaiver_calculatesAccurateDiscountAndNet() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setId(2L);
        sc.setName("Class 8");

        Section sec = new Section();
        sec.setId(3L);
        sec.setName("A");

        Student s1 = new Student();
        s1.setId(101L);
        s1.setStudentCode("STU-101");
        s1.setFullName("Rahim");

        Enrollment e1 = new Enrollment();
        e1.setId(1L);
        e1.setStudent(s1);
        e1.setSession(session);
        e1.setSchoolClass(sc);
        e1.setSection(sec);

        Student s2 = new Student();
        s2.setId(102L);
        s2.setStudentCode("STU-102");
        s2.setFullName("Karim (50% waiver)");

        Enrollment e2 = new Enrollment();
        e2.setId(2L);
        e2.setStudent(s2);
        e2.setSession(session);
        e2.setSchoolClass(sc);
        e2.setSection(sec);

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(e1);
        enrollments.add(e2);

        FeeHead tuitionHead = new FeeHead();
        tuitionHead.setId(5L);
        tuitionHead.setName("টিউশন ফি");
        tuitionHead.setCode("TUITION");

        FeeStructure fs = new FeeStructure();
        fs.setId(50L);
        fs.setSession(session);
        fs.setSchoolClass(sc);
        fs.setFeeHead(tuitionHead);
        fs.setAmount(new BigDecimal("2000.00"));
        fs.setFrequency(FeeFrequency.MONTHLY);
        fs.setActive(true);

        List<FeeStructure> structures = new ArrayList<>();
        structures.add(fs);

        StudentWaiver w2 = new StudentWaiver();
        w2.setId(1L);
        w2.setStudent(s2);
        w2.setSession(session);
        w2.setFeeHead(tuitionHead);
        w2.setDiscountType(WaiverType.PERCENTAGE);
        w2.setDiscountValue(new BigDecimal("50.00"));
        w2.setActive(true);

        List<StudentWaiver> s2Waivers = new ArrayList<>();
        s2Waivers.add(w2);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(classRepository.findById(2L)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.findInClassAndSession(1L, 2L, EnrollmentStatus.ACTIVE)).thenReturn(enrollments);
        when(feeStructureRepository.findBySessionIdAndSchoolClassId(1L, 2L)).thenReturn(structures);
        when(invoiceRepository.countByInvoicePrefix("INV-202611")).thenReturn(0L);

        when(waiverRepository.findActiveByStudentAndSession(101L, 1L)).thenReturn(new ArrayList<>());
        when(waiverRepository.findActiveByStudentAndSession(102L, 1L)).thenReturn(s2Waivers);

        BulkInvoiceRequest req = new BulkInvoiceRequest(
                1L, 2L, null, "2026-11", "নভেম্বর ২০২৬ - টিউশন ফি",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), null
        );

        BulkInvoiceResponse res = service.generateBulkInvoices(req);
        assertNotNull(res);
        assertEquals(2, res.totalEligibleStudents());
        assertEquals(2, res.generatedInvoicesCount());
        assertEquals(0, res.skippedCount());
        // Student 1: 2000.00, Student 2: 2000 - 1000 = 1000.00 -> Total: 3000.00
        assertEquals(new BigDecimal("3000.00"), res.totalBilledAmount());
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void collectPayment_partialAndFullPayment_success() {
        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("STU-10");
        student.setFullName("Sadia");

        SchoolClass sc = new SchoolClass();
        sc.setName("Class 7");

        Section sec = new Section();
        sec.setName("B");

        Invoice inv = new Invoice();
        inv.setId(100L);
        inv.setInvoiceNumber("INV-202611-00001");
        inv.setTitle("নভেম্বর টিউশন");
        inv.setBillingMonth("2026-11");
        inv.setStudent(student);
        inv.setSchoolClass(sc);
        inv.setSection(sec);
        inv.setNetAmount(new BigDecimal("1500.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setDueAmount(new BigDecimal("1500.00"));
        inv.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(inv));
        when(paymentRepository.countByReceiptPrefix(any())).thenReturn(0L);
        when(paymentRepository.save(any(FeePayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Part 1: আংশিক পেমেন্ট ১০০০ টাকা
        PaymentCollectionRequest req1 = new PaymentCollectionRequest(
                100L, new BigDecimal("1000.00"), PaymentMethod.BKASH, "TRX12345", "বিকাশ আংশিক পেমেন্ট"
        );
        PaymentReceiptResponse receipt1 = service.collectPayment(req1, "admin");

        assertNotNull(receipt1);
        assertEquals(new BigDecimal("1000.00"), receipt1.amount());
        assertEquals(new BigDecimal("500.00"), receipt1.remainingDueAmount());
        assertEquals(InvoiceStatus.PARTIAL, receipt1.invoiceStatus());

        // Part 2: বাকি ৫০০ টাকা সম্পূর্ণ পরিশোধ
        PaymentCollectionRequest req2 = new PaymentCollectionRequest(
                100L, new BigDecimal("500.00"), PaymentMethod.CASH, null, "বাকি পরিশোধ"
        );
        PaymentReceiptResponse receipt2 = service.collectPayment(req2, "admin");

        assertNotNull(receipt2);
        assertEquals(new BigDecimal("500.00"), receipt2.amount());
        assertEquals(new BigDecimal("0.00"), receipt2.remainingDueAmount());
        assertEquals(InvoiceStatus.PAID, receipt2.invoiceStatus());
    }

    @Test
    void collectPayment_overPayment_throwsBadRequest() {
        Invoice inv = new Invoice();
        inv.setId(200L);
        inv.setNetAmount(new BigDecimal("1000.00"));
        inv.setPaidAmount(new BigDecimal("500.00"));
        inv.setDueAmount(new BigDecimal("500.00"));
        inv.setStatus(InvoiceStatus.PARTIAL);

        when(invoiceRepository.findByIdWithDetails(200L)).thenReturn(Optional.of(inv));

        PaymentCollectionRequest req = new PaymentCollectionRequest(
                200L, new BigDecimal("700.00"), PaymentMethod.CASH, null, null
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.collectPayment(req, "admin"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void applyLateFine_success() {
        Invoice inv = new Invoice();
        inv.setId(300L);
        inv.setFineAmount(BigDecimal.ZERO);
        inv.setNetAmount(new BigDecimal("1200.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setDueAmount(new BigDecimal("1200.00"));
        inv.setStatus(InvoiceStatus.UNPAID);

        Student student = new Student();
        student.setId(10L);
        student.setStudentCode("2026-00010");
        student.setFullName("Amina");
        inv.setStudent(student);

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");
        inv.setSession(session);

        SchoolClass sc = new SchoolClass();
        sc.setId(10L);
        sc.setName("Class 10");
        inv.setSchoolClass(sc);

        Section sec = new Section();
        sec.setId(101L);
        sec.setName("Section A");
        inv.setSection(sec);

        when(invoiceRepository.findByIdWithDetails(300L)).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        ApplyFineRequest req = new ApplyFineRequest(new BigDecimal("50.00"), "বিলম্ব ফি");
        InvoiceResponse res = service.applyLateFine(300L, req);

        assertNotNull(res);
        assertEquals(new BigDecimal("50.00"), res.fineAmount());
        assertEquals(new BigDecimal("1250.00"), res.netAmount());
        assertEquals(new BigDecimal("1250.00"), res.dueAmount());
        verify(auditService).log(any(), any(), any(), any());
    }
}
