package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentCollectionRequest;
import com.sms.studentmanagement.dto.FeeCollectionDtos.PaymentReceiptResponse;
import com.sms.studentmanagement.dto.PaymentDtos.CreatePaymentIntentRequest;
import com.sms.studentmanagement.dto.PaymentDtos.ExecutePaymentRequest;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentIntentResponse;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentReceiptSummary;
import com.sms.studentmanagement.dto.PaymentDtos.PaymentTransactionDetail;
import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceStatus;
import com.sms.studentmanagement.entity.PaymentGatewayProvider;
import com.sms.studentmanagement.entity.PaymentMethod;
import com.sms.studentmanagement.entity.PaymentTransaction;
import com.sms.studentmanagement.entity.PaymentTransactionStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.InvoiceRepository;
import com.sms.studentmanagement.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnlinePaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(OnlinePaymentGatewayService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final FeeCollectionService feeCollectionService;
    private final AuditService auditService;

    /**
     * পেমেন্ট ইনটেন্ট তৈরি (Create Payment Intent)
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request, String currentUsername) {
        // ১. Idempotency Check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<PaymentTransaction> existing = paymentTransactionRepository.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existing.isPresent()) {
                PaymentTransaction pt = existing.get();
                Invoice inv = pt.getInvoice();
                return new PaymentIntentResponse(
                        pt.getTrxId(),
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        inv.getStudent().getFullName(),
                        pt.getAmount(),
                        pt.getGatewayProvider(),
                        pt.getStatus(),
                        pt.getGatewayPaymentId(),
                        pt.getPaymentUrl(),
                        "পূর্বের বিদ্যমান পেমেন্ট ইনটেন্ট ফিরিয়ে দেওয়া হয়েছে (Idempotent response)"
                );
            }
        }

        // ২. ইনভয়েস ভ্যালিডেশন
        Invoice invoice = invoiceRepository.findByIdWithDetails(request.invoiceId())
                .orElseThrow(() -> new NotFoundException("ইনভয়েস পাওয়া যায়নি: " + request.invoiceId()));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "বাতিলকৃত ইনভয়েসে অনলাইন পেমেন্ট সম্ভব নয়।");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "এই ইনভয়েসের ফি সম্পূর্ণ পরিশোধিত, কোনো বকেয়া নেই।");
        }

        PaymentGatewayProvider provider = request.gatewayProvider() != null
                ? request.gatewayProvider()
                : PaymentGatewayProvider.BKASH;

        // ৩. অনন্য ট্রানজেকশন আইডি জেনারেট
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String trxId = "TRX-" + provider.name() + "-" + uniqueId;
        String gatewayPaymentId = provider.name().toLowerCase() + "_pay_" + uniqueId;
        String mockPaymentUrl = "https://checkout.sandbox.bKash.com/payment/" + gatewayPaymentId;

        // ৪. ট্রানজেকশন রেকর্ড সংরক্ষণ
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTrxId(trxId);
        transaction.setInvoice(invoice);
        transaction.setGatewayProvider(provider);
        transaction.setAmount(invoice.getDueAmount().setScale(2, RoundingMode.HALF_UP));
        transaction.setStatus(PaymentTransactionStatus.INITIATED);
        transaction.setGatewayPaymentId(gatewayPaymentId);
        transaction.setCustomerPhone(request.customerPhone() != null ? request.customerPhone().trim() : null);
        transaction.setIdempotencyKey(request.idempotencyKey() != null ? request.idempotencyKey().trim() : null);
        transaction.setPaymentUrl(mockPaymentUrl);
        transaction.setPayerReference(invoice.getStudent().getStudentCode());

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        auditService.log("PAYMENT_INTENT_CREATED", "PAYMENT_TRANSACTION", saved.getId(),
                "শিক্ষার্থী: " + invoice.getStudent().getFullName() + " | ইনভয়েস: " + invoice.getInvoiceNumber()
                        + " | পরিমাণ: ৳" + saved.getAmount() + " | গেটওয়ে: " + provider.name());

        return new PaymentIntentResponse(
                saved.getTrxId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStudent().getFullName(),
                saved.getAmount(),
                saved.getGatewayProvider(),
                saved.getStatus(),
                saved.getGatewayPaymentId(),
                saved.getPaymentUrl(),
                "অনলাইন পেমেন্ট ইনটেন্ট সফলভাবে তৈরি হয়েছে।"
        );
    }

    /**
     * পেমেন্ট এক্সেকিউট ও ফি রসিদ তৈরি (Execute Payment & Complete Settlement)
     */
    @Transactional
    public PaymentReceiptSummary executePayment(ExecutePaymentRequest request, String currentUsername) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTrxId(request.trxId())
                .orElseThrow(() -> new NotFoundException("পেমেন্ট ট্রানজেকশন পাওয়া যায়নি: " + request.trxId()));

        if (transaction.getStatus() == PaymentTransactionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "এই লেনদেনটি ইতিমধ্যে সম্পন্ন হয়েছে।");
        }
        if (transaction.getStatus() == PaymentTransactionStatus.CANCELLED || transaction.getStatus() == PaymentTransactionStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "এই লেনদেনটি বাতিল অথবা ব্যর্থ হয়েছে। নতুন করে চেষ্টা করুন।");
        }

        // গেটওয়ে সিমুলেশন ভ্যালিডেশন (OTP & PIN)
        if (request.pin() == null || request.pin().trim().length() < 4) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage("ভুল পিন (Invalid PIN) প্রদান করা হয়েছে।");
            paymentTransactionRepository.save(transaction);
            throw new ApiException(HttpStatus.BAD_REQUEST, "সঠিক ৪ বা ৫ ডিজিটের পিন (PIN) প্রদান করুন।");
        }

        // গেটওয়ে TrxID তৈরি
        String gatewayTrxId = "GW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        transaction.setGatewayTrxId(gatewayTrxId);
        transaction.setStatus(PaymentTransactionStatus.COMPLETED);
        transaction.setVerifiedAt(Instant.now());
        paymentTransactionRepository.save(transaction);

        // ফি কালেকশন সার্ভিস কল করে অফিসিয়াল রসিদ (FeePayment) ও ইনভয়েস স্ট্যাটাস আপডেট
        PaymentMethod paymentMethod = transaction.getGatewayProvider() == PaymentGatewayProvider.BKASH
                ? PaymentMethod.BKASH
                : PaymentMethod.CARD;

        PaymentCollectionRequest collectionRequest = new PaymentCollectionRequest(
                transaction.getInvoice().getId(),
                transaction.getAmount(),
                paymentMethod,
                transaction.getTrxId(),
                "অনলাইন পেমেন্ট (" + transaction.getGatewayProvider().name() + " Gateway TrxID: " + gatewayTrxId + ")"
        );

        PaymentReceiptResponse receipt = feeCollectionService.collectPayment(
                collectionRequest,
                currentUsername != null ? currentUsername : "ONLINE_CHECKOUT"
        );

        auditService.log("ONLINE_PAYMENT_SETTLED", "PAYMENT_TRANSACTION", transaction.getId(),
                "রসিদ নং: " + receipt.receiptNumber() + " | TrxID: " + transaction.getTrxId()
                        + " | গেটওয়ে TrxID: " + gatewayTrxId + " | পরিমাণ: ৳" + transaction.getAmount());

        return new PaymentReceiptSummary(
                transaction.getTrxId(),
                receipt.receiptNumber(),
                transaction.getInvoice().getId(),
                receipt.amount(),
                receipt.remainingDueAmount(),
                gatewayTrxId,
                receipt.paymentDate(),
                "পেমেন্ট সফলভাবে সম্পন্ন হয়েছে এবং রসিদ তৈরি হয়েছে।"
        );
    }

    /**
     * ট্রানজেকশন অনুসন্ধান (Query Payment Status)
     */
    @Transactional(readOnly = true)
    public PaymentTransactionDetail queryPayment(String trxId) {
        PaymentTransaction pt = paymentTransactionRepository.findByTrxId(trxId)
                .orElseThrow(() -> new NotFoundException("পেমেন্ট ট্রানজেকশন পাওয়া যায়নি: " + trxId));

        Invoice inv = pt.getInvoice();
        return new PaymentTransactionDetail(
                pt.getId(),
                pt.getTrxId(),
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getStudent().getFullName(),
                pt.getAmount(),
                pt.getGatewayProvider(),
                pt.getStatus(),
                pt.getGatewayPaymentId(),
                pt.getGatewayTrxId(),
                pt.getCustomerPhone(),
                pt.getCreatedAt(),
                pt.getVerifiedAt()
        );
    }

    /**
     * ইনভয়েসের সকল অনলাইন ট্রানজেকশন তালিকা
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDetail> getInvoiceTransactions(Long invoiceId) {
        List<PaymentTransaction> list = paymentTransactionRepository.findByInvoiceId(invoiceId);
        List<PaymentTransactionDetail> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            PaymentTransaction pt = list.get(i);
            Invoice inv = pt.getInvoice();
            result.add(new PaymentTransactionDetail(
                    pt.getId(),
                    pt.getTrxId(),
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getStudent().getFullName(),
                    pt.getAmount(),
                    pt.getGatewayProvider(),
                    pt.getStatus(),
                    pt.getGatewayPaymentId(),
                    pt.getGatewayTrxId(),
                    pt.getCustomerPhone(),
                    pt.getCreatedAt(),
                    pt.getVerifiedAt()
            ));
        }
        return result;
    }
}
