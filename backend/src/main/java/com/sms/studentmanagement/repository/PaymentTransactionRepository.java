package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTrxId(String trxId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByGatewayPaymentId(String gatewayPaymentId);

    @Query("""
            select pt from PaymentTransaction pt
            join fetch pt.invoice inv
            where inv.id = :invoiceId
            order by pt.createdAt desc
            """)
    List<PaymentTransaction> findByInvoiceId(@Param("invoiceId") Long invoiceId);
}
