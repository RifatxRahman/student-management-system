package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.AccountDtos.AccountsSummaryResponse;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryResponse;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseResponse;
import com.sms.studentmanagement.service.AccountsService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountsController {

    private final AccountsService accountsService;

    // ==================== Expense Categories ====================

    @GetMapping("/expense-categories")
    public List<ExpenseCategoryResponse> listCategories(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return accountsService.listCategories(activeOnly);
    }

    @GetMapping("/expense-categories/{id}")
    public ExpenseCategoryResponse getCategory(@PathVariable Long id) {
        return accountsService.getCategory(id);
    }

    @PostMapping("/expense-categories")
    public ResponseEntity<ExpenseCategoryResponse> createCategory(
            @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountsService.createCategory(request));
    }

    @PutMapping("/expense-categories/{id}")
    public ExpenseCategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCategoryRequest request) {
        return accountsService.updateCategory(id, request);
    }

    @DeleteMapping("/expense-categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        accountsService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Expenses ====================

    @GetMapping("/expenses")
    public List<ExpenseResponse> listExpenses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return accountsService.listExpenses(categoryId, startDate, endDate);
    }

    @GetMapping("/expenses/{id}")
    public ExpenseResponse getExpense(@PathVariable Long id) {
        return accountsService.getExpense(id);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.status(HttpStatus.CREATED).body(accountsService.createExpense(request, username));
    }

    @PutMapping("/expenses/{id}")
    public ExpenseResponse updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return accountsService.updateExpense(id, request);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        accountsService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Accounts Summary ====================

    @GetMapping("/accounts/summary")
    public AccountsSummaryResponse getAccountsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return accountsService.getAccountsSummary(startDate, endDate);
    }
}
