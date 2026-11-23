package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.FeeCollectionDtos.ApplyFineRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.BulkInvoiceResponse;
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
import com.sms.studentmanagement.entity.FeeFrequency;
import com.sms.studentmanagement.entity.FeeHead;
import com.sms.studentmanagement.entity.FeePayment;
import com.sms.studentmanagement.entity.FeeStructure;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceItem;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentWaiver;
import com.sms.studentmanagement.entity.WaiverType;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeeCollectionService {

    private final StudentWaiverRepository waiverRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final FeePaymentRepository paymentRepository;
    private final FeeHeadRepository feeHeadRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    // ==================== 1. Student Waiver Operations ====================

    @Transactional(readOnly = true)
    public List<StudentWaiverResponse> listWaivers(Long studentId, Long sessionId) {
        List<StudentWaiver> list;
        if (studentId != null && sessionId != null) {
            list = waiverRepository.findActiveByStudentAndSession(studentId, sessionId);
        } else if (studentId != null) {
            list = waiverRepository.findByStudentId(studentId);
        } else if (sessionId != null) {
            list = waiverRepository.findBySessionId(sessionId);
        } else {
            list = waiverRepository.findAll();
        }

        List<StudentWaiverResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(StudentWaiverResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional
    public StudentWaiverResponse createOrUpdateWaiver(StudentWaiverRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি: " + request.studentId()));
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ পাওয়া যায়নি: " + request.sessionId()));
        FeeHead feeHead = feeHeadRepository.findById(request.feeHeadId())
                .orElseThrow(() -> new NotFoundException("ফি খাত পাওয়া যায়নি: " + request.feeHeadId()));

        BigDecimal scaledDiscount = request.discountValue().setScale(2, RoundingMode.HALF_UP);
        if (request.discountType() == WaiverType.PERCENTAGE
                && (scaledDiscount.compareTo(BigDecimal.ZERO) < 0 || scaledDiscount.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শতকরা ছাড় ০% হতে ১০০% এর মধ্যে হতে হবে।");
        }

        Optional<StudentWaiver> existingOpt = waiverRepository.findByStudentIdAndSessionIdAndFeeHeadId(
                student.getId(), session.getId(), feeHead.getId());

        StudentWaiver waiver;
        if (existingOpt.isPresent()) {
            waiver = existingOpt.get();
        } else {
            waiver = new StudentWaiver();
            waiver.setStudent(student);
            waiver.setSession(session);
            waiver.setFeeHead(feeHead);
        }

        waiver.setDiscountType(request.discountType());
        waiver.setDiscountValue(scaledDiscount);
        waiver.setReason(request.reason() != null ? request.reason().trim() : null);
        waiver.setActive(request.active() == null || request.active());

        StudentWaiver saved = waiverRepository.save(waiver);
        auditService.log("STUDENT_WAIVER_SET", "STUDENT_WAIVER", saved.getId(),
                student.getFullName() + " (" + student.getStudentCode() + ") | " + feeHead.getName()
                        + " | ছাড়: " + saved.getDiscountValue() + (saved.getDiscountType() == WaiverType.PERCENTAGE ? "%" : "৳"));
        return StudentWaiverResponse.from(saved);
    }

    @Transactional
    public void deleteWaiver(Long id) {
        StudentWaiver w = waiverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ওয়েভার রেকর্ড পাওয়া যায়নি: " + id));
        waiverRepository.delete(w);
        auditService.log("STUDENT_WAIVER_DELETED", "STUDENT_WAIVER", id,
                w.getStudent().getFullName() + " | " + w.getFeeHead().getName());
    }

    // ==================== 2. Bulk Invoice Generation ====================

    @Transactional
    public BulkInvoiceResponse generateBulkInvoices(BulkInvoiceRequest request) {
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ পাওয়া যায়নি: " + request.sessionId()));
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("শ্রেণি পাওয়া যায়নি: " + request.classId()));

        List<Enrollment> enrollments;
        if (request.sectionId() != null) {
            enrollments = enrollmentRepository.findInSection(
                    request.sectionId(), request.issueDate(), request.dueDate());
        } else {
            enrollments = enrollmentRepository.findInClassAndSession(
                    session.getId(), schoolClass.getId(), EnrollmentStatus.ACTIVE);
        }

        if (enrollments.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "এই শ্রেণি/সেকশনে কোনো সক্রিয় শিক্ষার্থী পাওয়া যায়নি।");
        }

        // ফি কাঠামো সংগ্রহ
        List<FeeStructure> classStructures = feeStructureRepository.findBySessionIdAndSchoolClassId(
                session.getId(), schoolClass.getId());

        List<FeeStructure> applicableStructures = new ArrayList<>();
        if (request.feeHeadIds() != null && !request.feeHeadIds().isEmpty()) {
            for (int i = 0; i < classStructures.size(); i++) {
                FeeStructure fs = classStructures.get(i);
                if (fs.isActive() && request.feeHeadIds().contains(fs.getFeeHead().getId())) {
                    applicableStructures.add(fs);
                }
            }
        } else {
            // ডিফল্ট: মাসিক বেতন ও সংশ্লিষ্ট চার্জ
            for (int i = 0; i < classStructures.size(); i++) {
                FeeStructure fs = classStructures.get(i);
                if (fs.isActive() && fs.getFrequency() == FeeFrequency.MONTHLY) {
                    applicableStructures.add(fs);
                }
            }
        }

        if (applicableStructures.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    schoolClass.getName() + " শ্রেণির জন্য কোনো প্রযোজ্য ফি কাঠামো পাওয়া যায়নি। প্রথমে ফি কাঠামো সেট করুন।");
        }

        String monthPrefix = request.billingMonth().replace("-", "");
        long invoiceCountPrefix = invoiceRepository.countByInvoicePrefix("INV-" + monthPrefix);
        long sequence = invoiceCountPrefix + 1;

        int generatedCount = 0;
        int skippedCount = 0;
        BigDecimal totalBilled = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment enr = enrollments.get(i);
            Student student = enr.getStudent();

            // ডুপ্লিকেট ইনভয়েস প্রিভেনশন
            if (invoiceRepository.existsByStudentIdAndSessionIdAndBillingMonthAndTitle(
                    student.getId(), session.getId(), request.billingMonth(), request.title().trim())) {
                skippedCount++;
                continue;
            }

            // শিক্ষার্থীর সেশন ওয়েভার
            List<StudentWaiver> studentWaivers = waiverRepository.findActiveByStudentAndSession(
                    student.getId(), session.getId());

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(String.format("INV-%s-%05d", monthPrefix, sequence++));
            invoice.setStudent(student);
            invoice.setSession(session);
            invoice.setSchoolClass(enr.getSchoolClass());
            invoice.setSection(enr.getSection());
            invoice.setTitle(request.title().trim());
            invoice.setBillingMonth(request.billingMonth().trim());
            invoice.setIssueDate(request.issueDate());
            invoice.setDueDate(request.dueDate());

            BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalWaiver = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            for (int j = 0; j < applicableStructures.size(); j++) {
                FeeStructure fs = applicableStructures.get(j);
                BigDecimal baseAmt = fs.getAmount().setScale(2, RoundingMode.HALF_UP);

                // ওয়েভার গণনা
                BigDecimal itemWaiver = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                for (int k = 0; k < studentWaivers.size(); k++) {
                    StudentWaiver sw = studentWaivers.get(k);
                    if (sw.getFeeHead().getId().equals(fs.getFeeHead().getId())) {
                        if (sw.getDiscountType() == WaiverType.PERCENTAGE) {
                            itemWaiver = baseAmt.multiply(sw.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        } else {
                            itemWaiver = sw.getDiscountValue().min(baseAmt).setScale(2, RoundingMode.HALF_UP);
                        }
                        break;
                    }
                }

                BigDecimal netAmt = baseAmt.subtract(itemWaiver).setScale(2, RoundingMode.HALF_UP);

                InvoiceItem item = new InvoiceItem();
                item.setFeeHead(fs.getFeeHead());
                item.setFeeHeadName(fs.getFeeHead().getName());
                item.setAmount(baseAmt);
                item.setWaiverAmount(itemWaiver);
                item.setNetAmount(netAmt);
                invoice.addItem(item);

                subtotal = subtotal.add(baseAmt);
                totalWaiver = totalWaiver.add(itemWaiver);
            }

            BigDecimal net = subtotal.subtract(totalWaiver).setScale(2, RoundingMode.HALF_UP);
            invoice.setSubtotalAmount(subtotal);
            invoice.setWaiverAmount(totalWaiver);
            invoice.setFineAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            invoice.setNetAmount(net);
            invoice.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            invoice.setDueAmount(net);
            invoice.setStatus(net.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.UNPAID);

            invoiceRepository.save(invoice);
            totalBilled = totalBilled.add(net);
            generatedCount++;
        }

        auditService.log("INVOICES_BULK_GENERATED", "INVOICE", session.getId(),
                schoolClass.getName() + " | মাস: " + request.billingMonth() + " | তৈরি: "
                        + generatedCount + " টি, স্কিপ: " + skippedCount + " টি, মোট টাকা: ৳" + totalBilled);

        return new BulkInvoiceResponse(enrollments.size(), generatedCount, skippedCount, totalBilled);
    }

    // ==================== 3. Custom / Single Student Invoice ====================

    @Transactional
    public InvoiceResponse createCustomInvoice(CustomInvoiceRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি: " + request.studentId()));
        AcademicSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ পাওয়া যায়নি: " + request.sessionId()));

        List<Enrollment> enrollments = enrollmentRepository.findForStudents(
                List.of(student.getId()), session.getId(), EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "উক্ত শিক্ষাবর্ষে শিক্ষার্থীর কোনো সক্রিয় শ্রেণিভর্তি পাওয়া যায়নি।");
        }
        Enrollment currentEnrollment = enrollments.get(0);

        if (invoiceRepository.existsByStudentIdAndSessionIdAndBillingMonthAndTitle(
                student.getId(), session.getId(), request.billingMonth(), request.title().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "এই শিরোনাম ও বিলিং মাসের একটি ইনভয়েস ইতিমধ্যে বিদ্যমান।");
        }

        String monthPrefix = request.billingMonth().replace("-", "");
        long count = invoiceRepository.countByInvoicePrefix("INV-" + monthPrefix);
        String invNumber = String.format("INV-%s-%05d", monthPrefix, count + 1);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invNumber);
        invoice.setStudent(student);
        invoice.setSession(session);
        invoice.setSchoolClass(currentEnrollment.getSchoolClass());
        invoice.setSection(currentEnrollment.getSection());
        invoice.setTitle(request.title().trim());
        invoice.setBillingMonth(request.billingMonth().trim());
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes() != null ? request.notes().trim() : null);

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWaiver = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<CustomInvoiceItemRequest> items = request.items();
        for (int i = 0; i < items.size(); i++) {
            CustomInvoiceItemRequest itemReq = items.get(i);
            FeeHead feeHead = feeHeadRepository.findById(itemReq.feeHeadId())
                    .orElseThrow(() -> new NotFoundException("ফি খাত পাওয়া যায়নি: " + itemReq.feeHeadId()));

            BigDecimal baseAmt = itemReq.amount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal waiverAmt = itemReq.waiverAmount() != null
                    ? itemReq.waiverAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal netAmt = baseAmt.subtract(waiverAmt).setScale(2, RoundingMode.HALF_UP);

            InvoiceItem it = new InvoiceItem();
            it.setFeeHead(feeHead);
            it.setFeeHeadName(feeHead.getName());
            it.setAmount(baseAmt);
            it.setWaiverAmount(waiverAmt);
            it.setNetAmount(netAmt);
            invoice.addItem(it);

            subtotal = subtotal.add(baseAmt);
            totalWaiver = totalWaiver.add(waiverAmt);
        }

        BigDecimal net = subtotal.subtract(totalWaiver).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubtotalAmount(subtotal);
        invoice.setWaiverAmount(totalWaiver);
        invoice.setFineAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setNetAmount(net);
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setDueAmount(net);
        invoice.setStatus(net.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.UNPAID);

        Invoice saved = invoiceRepository.save(invoice);
        auditService.log("CUSTOM_INVOICE_CREATED", "INVOICE", saved.getId(),
                student.getFullName() + " | " + saved.getInvoiceNumber() + " | মোট: ৳" + saved.getNetAmount());

        return InvoiceResponse.from(saved);
    }

    // ==================== 4. Fee Payment Collection ====================

    @Transactional
    public PaymentReceiptResponse collectPayment(PaymentCollectionRequest request, String currentUsername) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(request.invoiceId())
                .orElseThrow(() -> new NotFoundException("ইনভয়েস পাওয়া যায়নি: " + request.invoiceId()));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "বাতিলকৃত ইনভয়েসে পেমেন্ট গ্রহণ সম্ভব নয়।");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "এই ইনভয়েসের ফি সম্পূর্ণ পরিশোধিত, কোনো বকেয়া নেই।");
        }

        BigDecimal payAmt = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (payAmt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "পরিশোধের পরিমাণ অবশ্যই ০ টাকার বেশি হতে হবে।");
        }
        if (payAmt.compareTo(invoice.getDueAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "প্রদেয় পরিমাণ মোট বকেয়ার (৳" + invoice.getDueAmount() + ") চেয়ে বেশি হতে পারে না।");
        }

        String monthPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long receiptCount = paymentRepository.countByReceiptPrefix("REC-" + monthPrefix);
        String receiptNumber = String.format("REC-%s-%05d", monthPrefix, receiptCount + 1);

        FeePayment payment = new FeePayment();
        payment.setReceiptNumber(receiptNumber);
        payment.setInvoice(invoice);
        payment.setPaymentDate(Instant.now());
        payment.setAmount(payAmt);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setTransactionId(request.transactionId() != null ? request.transactionId().trim() : null);
        payment.setCollectedBy(currentUsername != null ? currentUsername : "ADMIN");
        payment.setNotes(request.notes() != null ? request.notes().trim() : null);

        FeePayment savedPayment = paymentRepository.save(payment);

        // ইনভয়েসের বকেয়া ও পরিশোধিত পরিমাণ আপডেট
        BigDecimal updatedPaid = invoice.getPaidAmount().add(payAmt).setScale(2, RoundingMode.HALF_UP);
        BigDecimal updatedDue = invoice.getNetAmount().subtract(updatedPaid).setScale(2, RoundingMode.HALF_UP);

        invoice.setPaidAmount(updatedPaid);
        invoice.setDueAmount(updatedDue);

        if (updatedDue.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        invoiceRepository.save(invoice);

        auditService.log("FEE_PAYMENT_COLLECTED", "FEE_PAYMENT", savedPayment.getId(),
                invoice.getStudent().getFullName() + " (" + invoice.getStudent().getStudentCode() + ")"
                        + " | রসিদ নং: " + receiptNumber + " | ৳" + payAmt + " (" + payment.getPaymentMethod().getBanglaLabel() + ")");

        return PaymentReceiptResponse.from(savedPayment);
    }

    // ==================== 5. Late Fine Application ====================

    @Transactional
    public InvoiceResponse applyLateFine(Long invoiceId, ApplyFineRequest request) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new NotFoundException("ইনভয়েস পাওয়া যায়নি: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "বাতিলকৃত ইনভয়েসে লেট ফাইন প্রযোজ্য নয়।");
        }

        BigDecimal fine = request.fineAmount().setScale(2, RoundingMode.HALF_UP);
        invoice.setFineAmount(invoice.getFineAmount().add(fine).setScale(2, RoundingMode.HALF_UP));
        invoice.setNetAmount(invoice.getNetAmount().add(fine).setScale(2, RoundingMode.HALF_UP));
        invoice.setDueAmount(invoice.getDueAmount().add(fine).setScale(2, RoundingMode.HALF_UP));

        if (invoice.getStatus() == InvoiceStatus.PAID && fine.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        Invoice saved = invoiceRepository.save(invoice);
        auditService.log("FEE_FINE_APPLIED", "INVOICE", invoiceId,
                invoice.getStudent().getFullName() + " | ইনভয়েস: " + invoice.getInvoiceNumber() + " | জরিমানা: ৳" + fine);
        return InvoiceResponse.from(saved);
    }

    // ==================== 6. Queries & Search ====================

    @Transactional(readOnly = true)
    public List<InvoiceResponse> searchInvoices(
            Long sessionId, Long classId, Long sectionId, Long studentId, InvoiceStatus status, String billingMonth) {
        List<Invoice> list = invoiceRepository.searchInvoices(sessionId, classId, sectionId, studentId, status, billingMonth);
        List<InvoiceResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(InvoiceResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("ইনভয়েস পাওয়া যায়নি: " + id));
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse getPaymentReceipt(Long paymentId) {
        FeePayment payment = paymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new NotFoundException("পেমেন্ট রসিদ পাওয়া যায়নি: " + paymentId));
        return PaymentReceiptResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getStudentInvoices(Long studentId) {
        List<Invoice> list = invoiceRepository.findByStudentId(studentId);
        List<InvoiceResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(InvoiceResponse.from(list.get(i)));
        }
        return responses;
    }
}
