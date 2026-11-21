package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Expense;
import com.sms.studentmanagement.entity.ExpenseCategory;
import com.sms.studentmanagement.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AccountDtos {

    private AccountDtos() {}

    // ==================== Expense Category DTOs ====================

    public record ExpenseCategoryRequest(
            @NotBlank(message = "ব্যয়ের খাতের নাম দেওয়া আবশ্যক") String name,
            @NotBlank(message = "ব্যয়ের খাতের কোড দেওয়া আবশ্যক") String code,
            String description,
            Boolean active
    ) {}

    public record ExpenseCategoryResponse(
            Long id,
            String name,
            String code,
            String description,
            boolean active
    ) {
        public static ExpenseCategoryResponse from(ExpenseCategory cat) {
            return new ExpenseCategoryResponse(
                    cat.getId(),
                    cat.getName(),
                    cat.getCode(),
                    cat.getDescription(),
                    cat.isActive()
            );
        }
    }

    // ==================== Expense DTOs ====================

    public record ExpenseRequest(
            @NotNull(message = "ব্যয়ের খাত নির্বাচন আবশ্যক") Long categoryId,
            @NotBlank(message = "ব্যয়ের বিবরণ বা শিরোনাম আবশ্যক") String title,
            @NotNull(message = "খরচের তারিখ আবশ্যক") LocalDate expenseDate,
            @NotNull(message = "টাকার পরিমাণ আবশ্যক")
            @DecimalMin(value = "0.01", message = "খরচের পরিমাণ ০ টাকার বেশি হতে হবে")
            BigDecimal amount,
            @NotNull(message = "পরিশোধ মাধ্যম আবশ্যক") PaymentMethod paymentMethod,
            @NotBlank(message = "প্রাপকের নাম (কাকে প্রদান করা হয়েছে) আবশ্যক") String payeeName,
            String referenceNo,
            String notes
    ) {}

    public record ExpenseResponse(
            Long id,
            String voucherNumber,
            Long categoryId,
            String categoryName,
            String categoryCode,
            String title,
            LocalDate expenseDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String paymentMethodBangla,
            String payeeName,
            String referenceNo,
            String recordedBy,
            String notes,
            Instant createdAt
    ) {
        public static ExpenseResponse from(Expense exp) {
            return new ExpenseResponse(
                    exp.getId(),
                    exp.getVoucherNumber(),
                    exp.getCategory().getId(),
                    exp.getCategory().getName(),
                    exp.getCategory().getCode(),
                    exp.getTitle(),
                    exp.getExpenseDate(),
                    exp.getAmount(),
                    exp.getPaymentMethod(),
                    exp.getPaymentMethod().getBanglaLabel(),
                    exp.getPayeeName(),
                    exp.getReferenceNo(),
                    exp.getRecordedBy(),
                    exp.getNotes(),
                    exp.getCreatedAt()
            );
        }
    }

    // ==================== Accounts Summary DTOs ====================

    public record CategoryExpenseSummary(
            String categoryName,
            BigDecimal amount,
            BigDecimal percentage
    ) {}

    public record MethodIncomeSummary(
            PaymentMethod method,
            String methodBangla,
            BigDecimal amount,
            BigDecimal percentage
    ) {}

    public record AccountsSummaryResponse(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netSurplus,
            boolean isSurplus,
            BigDecimal totalOutstandingDues,
            long totalIncomeTransactions,
            long totalExpenseVouchers,
            List<CategoryExpenseSummary> expenseBreakdown,
            List<MethodIncomeSummary> incomeBreakdown
    ) {}
}
