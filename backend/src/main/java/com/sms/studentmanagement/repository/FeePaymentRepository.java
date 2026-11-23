package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.FeePayment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    @Query("SELECT p FROM FeePayment p JOIN FETCH p.invoice i JOIN FETCH i.student JOIN FETCH i.schoolClass JOIN FETCH i.section WHERE p.id = :id")
    Optional<FeePayment> findByIdWithDetails(@Param("id") Long id);

    Optional<FeePayment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT p FROM FeePayment p WHERE p.invoice.id = :invoiceId ORDER BY p.id DESC")
    List<FeePayment> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COUNT(p) FROM FeePayment p WHERE p.receiptNumber LIKE :prefix%")
    long countByReceiptPrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM FeePayment p JOIN FETCH p.invoice i JOIN FETCH i.student JOIN FETCH i.schoolClass WHERE p.paymentDate BETWEEN :start AND :end ORDER BY p.paymentDate DESC")
    List<FeePayment> findByPaymentDateRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM FeePayment p WHERE p.paymentDate BETWEEN :start AND :end")
    java.math.BigDecimal sumIncomeBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0)
        FROM FeePayment p
        WHERE p.paymentDate BETWEEN :start AND :end
        GROUP BY p.paymentMethod
        ORDER BY SUM(p.amount) DESC
    """)
    List<Object[]> sumIncomeByMethod(@Param("start") Instant start, @Param("end") Instant end);

    long countByPaymentDateBetween(Instant start, Instant end);
}
