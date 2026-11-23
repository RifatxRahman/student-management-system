package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.AccountDtos.AccountsSummaryResponse;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryResponse;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseResponse;
import com.sms.studentmanagement.entity.Expense;
import com.sms.studentmanagement.entity.ExpenseCategory;
import com.sms.studentmanagement.entity.PaymentMethod;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.ExpenseCategoryRepository;
import com.sms.studentmanagement.repository.ExpenseRepository;
import com.sms.studentmanagement.repository.FeePaymentRepository;
import com.sms.studentmanagement.repository.InvoiceRepository;
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
class AccountsServiceTest {

    @Mock
    ExpenseCategoryRepository categoryRepository;

    @Mock
    ExpenseRepository expenseRepository;

    @Mock
    FeePaymentRepository paymentRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    AuditService auditService;

    AccountsService service;

    @BeforeEach
    void setUp() {
        service = new AccountsService(
                categoryRepository,
                expenseRepository,
                paymentRepository,
                invoiceRepository,
                auditService
        );
    }

    @Test
    void createExpense_success() {
        ExpenseCategory cat = new ExpenseCategory();
        cat.setId(1L);
        cat.setName("ইউটিলিটি ও বিদ্যুৎ");
        cat.setCode("UTILITY");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(expenseRepository.countByVoucherPrefix("EXP-202611")).thenReturn(0L);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> {
            Expense e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        ExpenseRequest req = new ExpenseRequest(
                1L,
                "নভেম্বর বিদ্যুৎ বিল",
                LocalDate.of(2026, 11, 10),
                new BigDecimal("4500.50"),
                PaymentMethod.BANK,
                "DESCO",
                "BILL-9901",
                "অফিস ও ল্যাব"
        );

        ExpenseResponse res = service.createExpense(req, "admin");

        assertNotNull(res);
        assertEquals(10L, res.id());
        assertEquals("EXP-202611-00001", res.voucherNumber());
        assertEquals("ইউটিলিটি ও বিদ্যুৎ", res.categoryName());
        assertEquals(new BigDecimal("4500.50"), res.amount());
        assertEquals(PaymentMethod.BANK, res.paymentMethod());
        assertEquals("DESCO", res.payeeName());
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void createCategory_conflictCode_throwsException() {
        ExpenseCategoryRequest req = new ExpenseCategoryRequest("স্টেশনারি", "STATIONERY", "কাগজ ও কলম", true);
        when(categoryRepository.existsByNameIgnoreCase("স্টেশনারি")).thenReturn(false);
        when(categoryRepository.existsByCodeIgnoreCase("STATIONERY")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.createCategory(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_whenReferenced_throwsBadRequest() {
        ExpenseCategory cat = new ExpenseCategory();
        cat.setId(2L);
        cat.setName("বেতন");

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(cat));
        when(expenseRepository.existsByCategoryId(2L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteCategory(2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void getAccountsSummary_calculatesSurplusAndBreakdownsCorrectly() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(paymentRepository.sumIncomeBetween(any(), any())).thenReturn(new BigDecimal("50000.00"));
        when(paymentRepository.countByPaymentDateBetween(any(), any())).thenReturn(25L);

        when(expenseRepository.sumExpenseBetween(start, end)).thenReturn(new BigDecimal("30000.00"));
        when(expenseRepository.searchExpenses(null, start, end)).thenReturn(new ArrayList<>());

        when(invoiceRepository.sumTotalUnpaidDues()).thenReturn(new BigDecimal("15000.00"));

        List<Object[]> catRows = new ArrayList<>();
        catRows.add(new Object[]{"শিক্ষক বেতন", new BigDecimal("20000.00")});
        catRows.add(new Object[]{"বিদ্যুৎ বিল", new BigDecimal("10000.00")});
        when(expenseRepository.sumExpenseByCategory(start, end)).thenReturn(catRows);

        List<Object[]> methodRows = new ArrayList<>();
        methodRows.add(new Object[]{PaymentMethod.BKASH, new BigDecimal("30000.00")});
        methodRows.add(new Object[]{PaymentMethod.CASH, new BigDecimal("20000.00")});
        when(paymentRepository.sumIncomeByMethod(any(), any())).thenReturn(methodRows);

        AccountsSummaryResponse summary = service.getAccountsSummary(start, end);

        assertNotNull(summary);
        assertEquals(new BigDecimal("50000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("30000.00"), summary.totalExpense());
        assertEquals(new BigDecimal("20000.00"), summary.netSurplus());
        assertTrue(summary.isSurplus());
        assertEquals(new BigDecimal("15000.00"), summary.totalOutstandingDues());
        assertEquals(25L, summary.totalIncomeTransactions());

        assertEquals(2, summary.expenseBreakdown().size());
        assertEquals("শিক্ষক বেতন", summary.expenseBreakdown().get(0).categoryName());
        assertEquals(new BigDecimal("66.67"), summary.expenseBreakdown().get(0).percentage());
        assertEquals("বিদ্যুৎ বিল", summary.expenseBreakdown().get(1).categoryName());
        assertEquals(new BigDecimal("33.33"), summary.expenseBreakdown().get(1).percentage());

        assertEquals(2, summary.incomeBreakdown().size());
        assertEquals(PaymentMethod.BKASH, summary.incomeBreakdown().get(0).method());
        assertEquals(new BigDecimal("60.00"), summary.incomeBreakdown().get(0).percentage());
    }

    @Test
    void exportExpensesToExcel_returnsValidExcelBytes() {
        ExpenseCategory cat = new ExpenseCategory();
        cat.setId(1L);
        cat.setName("অফিস সামগ্রী");

        Expense exp = new Expense();
        exp.setId(10L);
        exp.setVoucherNumber("EXP-2026-0001");
        exp.setCategory(cat);
        exp.setTitle("কাগজ ও কলম ক্রয়");
        exp.setExpenseDate(LocalDate.now());
        exp.setAmount(new BigDecimal("2500.00"));
        exp.setPaymentMethod(PaymentMethod.CASH);
        exp.setPayeeName("মেসার্স পেপার হাউস");
        exp.setReferenceNo("REF-1234");
        exp.setNotes("বার্ষিক পরীক্ষার খাতা");

        when(expenseRepository.searchExpenses(any(), any(), any())).thenReturn(List.of(exp));

        byte[] bytes = service.exportExpensesToExcel(null, null, null);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
