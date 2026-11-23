package com.sms.studentmanagement.dto;

import java.util.List;

public final class ExcelDtos {

    private ExcelDtos() {
    }

    public record StudentImportError(
            int rowNumber,
            String studentName,
            String reason
    ) {}

    public record ImportedStudentSummary(
            String studentCode,
            String fullName,
            int rollNumber,
            String sectionName
    ) {}

    public record StudentImportResult(
            int totalRows,
            int successCount,
            int failureCount,
            List<ImportedStudentSummary> importedStudents,
            List<StudentImportError> errors,
            String message
    ) {}
}
