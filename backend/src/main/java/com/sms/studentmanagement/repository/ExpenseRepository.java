package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e JOIN FETCH e.category WHERE e.id = :id")
    Optional<Expense> findByIdWithCategory(@Param("id") Long id);

    Optional<Expense> findByVoucherNumber(String voucherNumber);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.voucherNumber LIKE :prefix%")
    long countByVoucherPrefix(@Param("prefix") String prefix);

    @Query("""
        SELECT e FROM Expense e
        JOIN FETCH e.category c
        WHERE (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:startDate IS NULL OR e.expenseDate >= :startDate)
          AND (:endDate IS NULL OR e.expenseDate <= :endDate)
        ORDER BY e.expenseDate DESC, e.id DESC
    """)
    List<Expense> searchExpenses(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumExpenseBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        SELECT e.category.name, COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.expenseDate BETWEEN :start AND :end
        GROUP BY e.category.name
        ORDER BY SUM(e.amount) DESC
    """)
    List<Object[]> sumExpenseByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
