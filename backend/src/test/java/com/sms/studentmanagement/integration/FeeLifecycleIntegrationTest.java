package com.sms.studentmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.FeeCollectionDtos.CustomInvoiceItemRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.CustomInvoiceRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.InvoiceResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentCollectionRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentReceiptResponse;
import com.sms.studentmanagement.dto.FeeCollectionDtos.StudentWaiverRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.StudentWaiverResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.FeeHead;
import com.sms.studentmanagement.entity.FeePayment;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceItem;
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
import com.sms.studentmanagement.service.AuditService;
import com.sms.studentmanagement.service.FeeCollectionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
class FeeLifecycleIntegrationTest {

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

    FeeCollectionService feeService;

    Student student;
    AcademicSession session;
    SchoolClass schoolClass;
    Section section;
    Enrollment enrollment;
    FeeHead tuitionHead;
    FeeHead examHead;

    @BeforeEach
    void setUp() {
        feeService = new FeeCollectionService(
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

        student = new Student();
        student.setId(10L);
        student.setStudentCode("2026-00010");
        student.setFullName("সুমাইয়া আক্তার");

        session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        schoolClass = new SchoolClass();
        schoolClass.setId(6L);
        schoolClass.setName("Class 6");

        section = new Section();
        section.setId(61L);
        section.setName("Section A");

        enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setStudent(student);
        enrollment.setSession(session);
        enrollment.setSchoolClass(schoolClass);
        enrollment.setSection(section);
        enrollment.setRollNumber(1);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        tuitionHead = new FeeHead();
        tuitionHead.setId(1L);
        tuitionHead.setName("মাসিক বেতন");
        tuitionHead.setCode("TUITION_FEE");

        examHead = new FeeHead();
        examHead.setId(2L);
        examHead.setName("পরীক্ষার ফি");
        examHead.setCode("EXAM_FEE");
    }

    @Test
    void endToEndFeeLifecycle_invoiceCreationToPartialAndFullPayment() {
        // ১. কাস্টম ইনভয়েস তৈরি
        List<CustomInvoiceItemRequest> items = new ArrayList<>();
        items.add(new CustomInvoiceItemRequest(1L, new BigDecimal("1000.00"), new BigDecimal("200.00"))); // বেতন ১০০০, মওকুফ ২০০ = ৮০০
        items.add(new CustomInvoiceItemRequest(2L, new BigDecimal("500.00"), BigDecimal.ZERO)); // পরীক্ষা ৫০০ = ৫০০
        // সর্বমোট নিট = ১৩০০.০০

        CustomInvoiceRequest invReq = new CustomInvoiceRequest(
                10L, 1L, "জানুয়ারি ২০২৬ বেতন ও পরীক্ষার ফি", "2026-01",
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 25), "নিয়মিত ফি", items
        );

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndSessionId(10L, 1L)).thenReturn(Optional.of(enrollment));
        when(invoiceRepository.findByStudentIdAndSessionIdAndBillingMonth(10L, 1L, "2026-01")).thenReturn(new ArrayList<>());
        when(feeHeadRepository.findById(1L)).thenReturn(Optional.of(tuitionHead));
        when(feeHeadRepository.findById(2L)).thenReturn(Optional.of(examHead));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(501L);
            return inv;
        });

        InvoiceResponse invResponse = feeService.createCustomInvoice(invReq);
        assertNotNull(invResponse);
        assertEquals(InvoiceStatus.UNPAID, invResponse.status());
        assertEquals(new BigDecimal("1500.00"), invResponse.subtotalAmount());
        assertEquals(new BigDecimal("200.00"), invResponse.waiverAmount());
        assertEquals(new BigDecimal("1300.00"), invResponse.netAmount());
        assertEquals(new BigDecimal("1300.00"), invResponse.dueAmount());

        // ২. আংশিক পেমেন্ট (৮০০ টাকা)
        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(501L);
        existingInvoice.setStudent(student);
        existingInvoice.setSession(session);
        existingInvoice.setInvoiceNumber("INV-202601-00001");
        existingInvoice.setNetAmount(new BigDecimal("1300.00"));
        existingInvoice.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        existingInvoice.setDueAmount(new BigDecimal("1300.00"));
        existingInvoice.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(existingInvoice));
        when(paymentRepository.save(any(FeePayment.class))).thenAnswer(invoc -> {
            FeePayment fp = invoc.getArgument(0);
            fp.setId(9001L);
            fp.setReceiptNumber("REC-202601-00001");
            return fp;
        });

        PaymentCollectionRequest partialPaymentReq = new PaymentCollectionRequest(
                501L, new BigDecimal("800.00"), PaymentMethod.CASH, null, LocalDate.now(), "প্রথম কিস্তি পরিশোধ"
        );

        PaymentReceiptResponse partialReceipt = feeService.collectPayment(partialPaymentReq, "accountant1");
        assertNotNull(partialReceipt);
        assertEquals(InvoiceStatus.PARTIALLY_PAID, existingInvoice.getStatus());
        assertEquals(new BigDecimal("800.00"), existingInvoice.getPaidAmount());
        assertEquals(new BigDecimal("500.00"), existingInvoice.getDueAmount());

        // ৩. চূড়ান্ত কিস্তি পেমেন্ট (বাকি ৫০০ টাকা)
        PaymentCollectionRequest finalPaymentReq = new PaymentCollectionRequest(
                501L, new BigDecimal("500.00"), PaymentMethod.BKASH, "TRX123456", LocalDate.now(), "চূড়ান্ত কিস্তি"
        );

        PaymentReceiptResponse finalReceipt = feeService.collectPayment(finalPaymentReq, "accountant1");
        assertNotNull(finalReceipt);
        assertEquals(InvoiceStatus.PAID, existingInvoice.getStatus());
        assertEquals(new BigDecimal("1300.00"), existingInvoice.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), existingInvoice.getDueAmount());
    }

    @Test
    void overpaymentRejectionEdgeCase_throwsBadRequest() {
        Invoice invoice = new Invoice();
        invoice.setId(502L);
        invoice.setDueAmount(new BigDecimal("500.00"));
        invoice.setPaidAmount(new BigDecimal("500.00"));
        invoice.setNetAmount(new BigDecimal("1000.00"));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);

        when(invoiceRepository.findByIdWithDetails(502L)).thenReturn(Optional.of(invoice));

        // প্রদেয় বকেয়া ৫০০ টাকার জায়গায় ৬০০ টাকা দিলে ব্যর্থ হবে
        PaymentCollectionRequest overpayReq = new PaymentCollectionRequest(
                502L, new BigDecimal("600.00"), PaymentMethod.CASH, null, LocalDate.now(), "অতিরিক্ত পেমেন্ট"
        );

        ApiException ex = assertThrows(ApiException.class, () -> feeService.collectPayment(overpayReq, "admin"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void negativeOrZeroPaymentRejectionEdgeCase_throwsBadRequest() {
        Invoice invoice = new Invoice();
        invoice.setId(503L);
        invoice.setDueAmount(new BigDecimal("500.00"));
        invoice.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findByIdWithDetails(503L)).thenReturn(Optional.of(invoice));

        PaymentCollectionRequest zeroReq = new PaymentCollectionRequest(
                503L, BigDecimal.ZERO, PaymentMethod.CASH, null, LocalDate.now(), "শূন্য পেমেন্ট"
        );
        PaymentCollectionRequest negativeReq = new PaymentCollectionRequest(
                503L, new BigDecimal("-100.00"), PaymentMethod.CASH, null, LocalDate.now(), "নেগেটিভ পেমেন্ট"
        );

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> feeService.collectPayment(zeroReq, "admin")).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> feeService.collectPayment(negativeReq, "admin")).getStatus());
    }

    @Test
    void paymentOnAlreadyPaidInvoiceRejectionEdgeCase_throwsBadRequest() {
        Invoice invoice = new Invoice();
        invoice.setId(504L);
        invoice.setDueAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findByIdWithDetails(504L)).thenReturn(Optional.of(invoice));

        PaymentCollectionRequest payReq = new PaymentCollectionRequest(
                504L, new BigDecimal("100.00"), PaymentMethod.CASH, null, LocalDate.now(), "পেইড ইনভয়েস"
        );

        ApiException ex = assertThrows(ApiException.class, () -> feeService.collectPayment(payReq, "admin"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void studentWaiverPercentageAndFixedCalculations_verified() {
        StudentWaiver percentageWaiver = new StudentWaiver();
        percentageWaiver.setId(1L);
        percentageWaiver.setStudent(student);
        percentageWaiver.setSession(session);
        percentageWaiver.setFeeHead(tuitionHead);
        percentageWaiver.setWaiverType(WaiverType.PERCENTAGE);
        percentageWaiver.setValue(new BigDecimal("50.00")); // ৫০% ছাড়

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(feeHeadRepository.findById(1L)).thenReturn(Optional.of(tuitionHead));
        when(waiverRepository.save(any(StudentWaiver.class))).thenReturn(percentageWaiver);

        StudentWaiverRequest req = new StudentWaiverRequest(10L, 1L, 1L, WaiverType.PERCENTAGE, new BigDecimal("50.00"), "অর্ধেক বেতন ছাড়");
        StudentWaiverResponse res = feeService.createOrUpdateWaiver(req);

        assertNotNull(res);
        assertEquals(WaiverType.PERCENTAGE, res.waiverType());
        assertEquals(new BigDecimal("50.00"), res.value());
    }
}
