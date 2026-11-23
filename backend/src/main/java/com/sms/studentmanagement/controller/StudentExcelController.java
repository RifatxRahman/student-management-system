package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.ExcelDtos.StudentImportResult;
import com.sms.studentmanagement.service.StudentExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentExcelController {

    private final StudentExcelService excelService;

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @GetMapping("/import-template")
    @PreAuthorize("hasAuthority('PERM_STUDENT_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] excelBytes = excelService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_admission_template.xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(excelBytes);
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_STUDENT_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<StudentImportResult> bulkImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("classId") Long classId,
            @RequestParam(value = "defaultSectionId", required = false) Long defaultSectionId) {

        StudentImportResult result = excelService.bulkImport(file, sessionId, classId, defaultSectionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_STUDENT_MANAGE') or hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STAFF')")
    public ResponseEntity<byte[]> exportStudents(
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("classId") Long classId,
            @RequestParam(value = "sectionId", required = false) Long sectionId) {

        byte[] excelBytes = excelService.exportStudents(sessionId, classId, sectionId);
        String filename = String.format("students_session_%d_class_%d.xlsx", sessionId, classId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(excelBytes);
    }
}
