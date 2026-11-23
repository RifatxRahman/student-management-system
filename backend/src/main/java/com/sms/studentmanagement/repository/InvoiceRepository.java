package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Invoice;
import com.sms.studentmanagement.entity.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i JOIN FETCH i.student JOIN FETCH i.session JOIN FETCH i.schoolClass JOIN FETCH i.section LEFT JOIN FETCH i.items WHERE i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByStudentIdAndSessionIdAndBillingMonthAndTitle(
            Long studentId, Long sessionId, String billingMonth, String title);

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.student s
        JOIN FETCH i.session sess
        JOIN FETCH i.schoolClass c
        JOIN FETCH i.section sec
        WHERE (:sessionId IS NULL OR i.session.id = :sessionId)
          AND (:classId IS NULL OR i.schoolClass.id = :classId)
          AND (:sectionId IS NULL OR i.section.id = :sectionId)
          AND (:studentId IS NULL OR i.student.id = :studentId)
          AND (:status IS NULL OR i.status = :status)
          AND (:billingMonth IS NULL OR i.billingMonth = :billingMonth)
        ORDER BY i.id DESC
    """)
    List<Invoice> searchInvoices(
            @Param("sessionId") Long sessionId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("studentId") Long studentId,
            @Param("status") InvoiceStatus status,
            @Param("billingMonth") String billingMonth
    );

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceNumber LIKE :prefix%")
    long countByInvoicePrefix(@Param("prefix") String prefix);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.student JOIN FETCH i.schoolClass JOIN FETCH i.section WHERE i.student.id = :studentId ORDER BY i.id DESC")
    List<Invoice> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i WHERE i.status != com.sms.studentmanagement.entity.InvoiceStatus.CANCELLED AND i.dueAmount > 0")
    java.math.BigDecimal sumTotalUnpaidDues();
}
