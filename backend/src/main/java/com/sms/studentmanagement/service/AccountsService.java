package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AccountDtos.AccountsSummaryResponse;
import com.sms.studentmanagement.dto.AccountDtos.CategoryExpenseSummary;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseCategoryResponse;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseRequest;
import com.sms.studentmanagement.dto.AccountDtos.ExpenseResponse;
import com.sms.studentmanagement.dto.AccountDtos.MethodIncomeSummary;
import com.sms.studentmanagement.entity.Expense;
import com.sms.studentmanagement.entity.ExpenseCategory;
import com.sms.studentmanagement.entity.PaymentMethod;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.ExpenseCategoryRepository;
import com.sms.studentmanagement.repository.ExpenseRepository;
import com.sms.studentmanagement.repository.FeePaymentRepository;
import com.sms.studentmanagement.repository.InvoiceRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountsService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final FeePaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;

    // ==================== Expense Category Operations ====================

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> listCategories(Boolean activeOnly) {
        List<ExpenseCategory> list;
        if (Boolean.TRUE.equals(activeOnly)) {
            list = categoryRepository.findByActiveTrueOrderByNameAsc();
        } else {
            list = categoryRepository.findAllByOrderByNameAsc();
        }

        List<ExpenseCategoryResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(ExpenseCategoryResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public ExpenseCategoryResponse getCategory(Long id) {
        ExpenseCategory cat = findCategory(id);
        return ExpenseCategoryResponse.from(cat);
    }

    @Transactional
    public ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request) {
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের ব্যয়ের খাত ইতিমধ্যে বিদ্যমান: " + name);
        }
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের ব্যয়ের খাত ইতিমধ্যে বিদ্যমান: " + code);
        }

        ExpenseCategory cat = new ExpenseCategory();
        cat.setName(name);
        cat.setCode(code);
        cat.setDescription(request.description() != null ? request.description().trim() : null);
        cat.setActive(request.active() == null || request.active());

        ExpenseCategory saved = categoryRepository.save(cat);
        auditService.log("EXPENSE_CATEGORY_CREATED", "EXPENSE_CATEGORY", saved.getId(),
                saved.getName() + " (" + saved.getCode() + ")");
        return ExpenseCategoryResponse.from(saved);
    }

    @Transactional
    public ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryRequest request) {
        ExpenseCategory cat = findCategory(id);
        String name = request.name().trim();
        String code = request.code().trim().toUpperCase();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই নামের ব্যয়ের খাত অন্য একটি রেকর্ডে বিদ্যমান: " + name);
        }
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "এই কোডের ব্যয়ের খাত অন্য একটি রেকর্ডে বিদ্যমান: " + code);
        }

        cat.setName(name);
        cat.setCode(code);
        cat.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.active() != null) {
            cat.setActive(request.active());
        }

        ExpenseCategory updated = categoryRepository.save(cat);
        auditService.log("EXPENSE_CATEGORY_UPDATED", "EXPENSE_CATEGORY", updated.getId(),
                updated.getName() + " (" + updated.getCode() + ")");
        return ExpenseCategoryResponse.from(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ExpenseCategory cat = findCategory(id);
        if (expenseRepository.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "এই খাতের অধীনে খরচ ভাউচার রয়েছে। খাতটি মুছে ফেলা যাবে না, প্রয়োজনে নিষ্ক্রিয় করুন।");
        }
        categoryRepository.delete(cat);
        auditService.log("EXPENSE_CATEGORY_DELETED", "EXPENSE_CATEGORY", id, cat.getName());
    }

    // ==================== Expense Voucher Operations ====================

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(Long categoryId, LocalDate startDate, LocalDate endDate) {
        List<Expense> list = expenseRepository.searchExpenses(categoryId, startDate, endDate);
        List<ExpenseResponse> responses = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            responses.add(ExpenseResponse.from(list.get(i)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long id) {
        Expense exp = expenseRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("খরচ ভাউচার পাওয়া যায়নি: " + id));
        return ExpenseResponse.from(exp);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, String currentUsername) {
        ExpenseCategory category = findCategory(request.categoryId());

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "খরচের পরিমাণ ০ টাকার বেশি হতে হবে।");
        }

        String monthPrefix = request.expenseDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = expenseRepository.countByVoucherPrefix("EXP-" + monthPrefix);
        String voucherNumber = String.format("EXP-%s-%05d", monthPrefix, count + 1);

        Expense exp = new Expense();
        exp.setVoucherNumber(voucherNumber);
        exp.setCategory(category);
        exp.setTitle(request.title().trim());
        exp.setExpenseDate(request.expenseDate());
        exp.setAmount(amount);
        exp.setPaymentMethod(request.paymentMethod());
        exp.setPayeeName(request.payeeName().trim());
        exp.setReferenceNo(request.referenceNo() != null ? request.referenceNo().trim() : null);
        exp.setRecordedBy(currentUsername != null ? currentUsername : "ADMIN");
        exp.setNotes(request.notes() != null ? request.notes().trim() : null);

        Expense saved = expenseRepository.save(exp);
        auditService.log("EXPENSE_CREATED", "EXPENSE", saved.getId(),
                saved.getVoucherNumber() + " | " + category.getName() + " | " + saved.getTitle()
                        + " | ৳" + saved.getAmount() + " (" + saved.getPayeeName() + ")");

        return ExpenseResponse.from(saved);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        Expense exp = expenseRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("খরচ ভাউচার পাওয়া যায়নি: " + id));
        ExpenseCategory category = findCategory(request.categoryId());

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "খরচের পরিমাণ ০ টাকার বেশি হতে হবে।");
        }

        exp.setCategory(category);
        exp.setTitle(request.title().trim());
        exp.setExpenseDate(request.expenseDate());
        exp.setAmount(amount);
        exp.setPaymentMethod(request.paymentMethod());
        exp.setPayeeName(request.payeeName().trim());
        exp.setReferenceNo(request.referenceNo() != null ? request.referenceNo().trim() : null);
        exp.setNotes(request.notes() != null ? request.notes().trim() : null);

        Expense updated = expenseRepository.save(exp);
        auditService.log("EXPENSE_UPDATED", "EXPENSE", updated.getId(),
                updated.getVoucherNumber() + " | " + category.getName() + " | ৳" + updated.getAmount());

        return ExpenseResponse.from(updated);
    }

    @Transactional
    public void deleteExpense(Long id) {
        Expense exp = expenseRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("খরচ ভাউচার পাওয়া যায়নি: " + id));
        expenseRepository.delete(exp);
        auditService.log("EXPENSE_DELETED", "EXPENSE", id,
                exp.getVoucherNumber() + " | " + exp.getTitle() + " | ৳" + exp.getAmount());
    }

    // ==================== Accounts Summary & Net Surplus ====================

    @Transactional(readOnly = true)
    public AccountsSummaryResponse getAccountsSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শুরুর তারিখ শেষের তারিখের চেয়ে বড় হতে পারে না।");
        }

        Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        // ১. মোট আয় (ফি সংগ্রহ)
        BigDecimal totalIncome = paymentRepository.sumIncomeBetween(startInstant, endInstant);
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalIncome = totalIncome.setScale(2, RoundingMode.HALF_UP);
        }
        long incomeTxnCount = paymentRepository.countByPaymentDateBetween(startInstant, endInstant);

        // ২. মোট ব্যয় (পরিচালন খরচ)
        BigDecimal totalExpense = expenseRepository.sumExpenseBetween(start, end);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalExpense = totalExpense.setScale(2, RoundingMode.HALF_UP);
        }
        long expenseVoucherCount = expenseRepository.searchExpenses(null, start, end).size();

        // ৩. নিট লাভ / উদ্বৃত্ত (Surplus / Deficit)
        BigDecimal netSurplus = totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP);
        boolean isSurplus = netSurplus.compareTo(BigDecimal.ZERO) >= 0;

        // ৪. মোট শিক্ষার্থী বকেয়া
        BigDecimal totalOutstandingDues = invoiceRepository.sumTotalUnpaidDues();
        if (totalOutstandingDues == null) {
            totalOutstandingDues = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalOutstandingDues = totalOutstandingDues.setScale(2, RoundingMode.HALF_UP);
        }

        // ৫. ব্যয়ের খাতভিত্তিক ব্রেকডাউন
        List<Object[]> catRows = expenseRepository.sumExpenseByCategory(start, end);
        List<CategoryExpenseSummary> expenseBreakdown = new ArrayList<>();
        for (int i = 0; i < catRows.size(); i++) {
            Object[] row = catRows.get(i);
            String catName = (String) row[0];
            BigDecimal catAmount = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                percentage = catAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP);
            }
            expenseBreakdown.add(new CategoryExpenseSummary(catName, catAmount, percentage));
        }

        // ৬. আয়ের মাধ্যমভিত্তিক ব্রেকডাউন
        List<Object[]> methodRows = paymentRepository.sumIncomeByMethod(startInstant, endInstant);
        List<MethodIncomeSummary> incomeBreakdown = new ArrayList<>();
        for (int i = 0; i < methodRows.size(); i++) {
            Object[] row = methodRows.get(i);
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal methodAmount = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                percentage = methodAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalIncome, 2, RoundingMode.HALF_UP);
            }
            incomeBreakdown.add(new MethodIncomeSummary(method, method.getBanglaLabel(), methodAmount, percentage));
        }

        return new AccountsSummaryResponse(
                start,
                end,
                totalIncome,
                totalExpense,
                netSurplus,
                isSurplus,
                totalOutstandingDues,
                incomeTxnCount,
                expenseVoucherCount,
                expenseBreakdown,
                incomeBreakdown
        );
    }

    private ExpenseCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ব্যয়ের খাত পাওয়া যায়নি: " + id));
    }

    @Transactional(readOnly = true)
    public byte[] exportExpensesToExcel(Long categoryId, LocalDate startDate, LocalDate endDate) {
        List<Expense> list = expenseRepository.searchExpenses(categoryId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Expenses");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setThinBorders(dataStyle);

            CellStyle numStyle = workbook.createCellStyle();
            numStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            setThinBorders(numStyle);

            String[] headers = {
                    "ক্রমিক", "ভাউচার নং", "ব্যয়ের খাত", "বিবরণ/শিরোনাম",
                    "তারিখ", "পরিমাণ (৳)", "পেমেন্ট মাধ্যম", "প্রাপক",
                    "রেফারেন্স নং", "নোট"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < list.size(); i++) {
                Expense exp = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.setHeightInPoints(20);

                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(exp.getVoucherNumber() != null ? exp.getVoucherNumber() : "");
                row.createCell(2).setCellValue(exp.getCategory() != null ? exp.getCategory().getName() : "");
                row.createCell(3).setCellValue(exp.getTitle() != null ? exp.getTitle() : "");
                row.createCell(4).setCellValue(exp.getExpenseDate() != null ? exp.getExpenseDate().toString() : "");

                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(exp.getAmount() != null ? exp.getAmount().doubleValue() : 0.0);
                amountCell.setCellStyle(numStyle);

                row.createCell(6).setCellValue(exp.getPaymentMethod() != null ? exp.getPaymentMethod().getBanglaLabel() : "");
                row.createCell(7).setCellValue(exp.getPayeeName() != null ? exp.getPayeeName() : "");
                row.createCell(8).setCellValue(exp.getReferenceNo() != null ? exp.getReferenceNo() : "");
                row.createCell(9).setCellValue(exp.getNotes() != null ? exp.getNotes() : "");

                for (int col = 0; col < headers.length; col++) {
                    if (col != 5) {
                        Cell c = row.getCell(col);
                        if (c != null) c.setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 3200));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "খরচের এক্সেল ফাইল তৈরিতে ব্যর্থ হয়েছে: " + ex.getMessage());
        }
    }

    private void setThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
