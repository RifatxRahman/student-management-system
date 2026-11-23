package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.ExcelDtos.ImportedStudentSummary;
import com.sms.studentmanagement.dto.ExcelDtos.StudentImportError;
import com.sms.studentmanagement.dto.ExcelDtos.StudentImportResult;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Gender;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.GuardianRelation;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.GuardianRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.util.DhakaTime;
import com.sms.studentmanagement.util.PhoneNumbers;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentExcelService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StudentCodeGenerator codeGenerator;
    private final AuditService auditService;

    private static final String[] TEMPLATE_HEADERS = {
            "শিক্ষার্থীর পুরো নাম *",
            "লিঙ্গ (MALE/FEMALE) *",
            "জন্মতারিখ (YYYY-MM-DD) *",
            "শিক্ষার্থীর ফোন নম্বর",
            "বর্তমান ঠিকানা",
            "অভিভাবকের নাম *",
            "অভিভাবকের ফোন নম্বর *",
            "সম্পর্ক (FATHER/MOTHER/GUARDIAN) *",
            "শাখা/সেকশন (ঐচ্ছিক)",
            "রোল নম্বর (ঐচ্ছিক)"
    };

    /**
     * নমুনা এক্সেল টেমপ্লেট তৈরি
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("শিক্ষার্থী ভর্তি টেমপ্লেট");

            // হেডার স্টাইল
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
            setBorders(headerStyle);

            // ডাটা সেল স্টাইল
            CellStyle dataStyle = workbook.createCellStyle();
            setBorders(dataStyle);

            // হেডার রো
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // নমুনা ডাটা রো ১
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("আব্দুল্লাহ আল মামুন");
            row1.createCell(1).setCellValue("MALE");
            row1.createCell(2).setCellValue("2012-05-15");
            row1.createCell(3).setCellValue("01711223344");
            row1.createCell(4).setCellValue("মিরপুর-১০, ঢাকা");
            row1.createCell(5).setCellValue("মোঃ রফিকুল ইসলাম");
            row1.createCell(6).setCellValue("01811223344");
            row1.createCell(7).setCellValue("FATHER");
            row1.createCell(8).setCellValue("A");
            row1.createCell(9).setCellValue("1");

            // নমুনা ডাটা রো ২
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("সুমাইয়া আক্তার");
            row2.createCell(1).setCellValue("FEMALE");
            row2.createCell(2).setCellValue("2013-02-20");
            row2.createCell(3).setCellValue("");
            row2.createCell(4).setCellValue("উত্তরা সেক্টর ৪, ঢাকা");
            row2.createCell(5).setCellValue("মোসাম্মৎ আয়েশা বেগম");
            row2.createCell(6).setCellValue("01911223344");
            row2.createCell(7).setCellValue("MOTHER");
            row2.createCell(8).setCellValue("A");
            row2.createCell(9).setCellValue("2");

            for (int r = 1; r <= 2; r++) {
                Row row = sheet.getRow(r);
                for (int c = 0; c < TEMPLATE_HEADERS.length; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 4000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("এক্সেল টেমপ্লেট তৈরিতে ত্রুটি", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "এক্সেল টেমপ্লেট তৈরিতে সমস্যা হয়েছে");
        }
    }

    /**
     * এক্সেল ফাইল থেকে শিক্ষার্থীদের বাল্ক ইমপোর্ট
     */
    @Transactional
    public StudentImportResult bulkImport(MultipartFile file, Long sessionId, Long classId, Long defaultSectionId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "কোনো এক্সেল ফাইল প্রদান করা হয়নি");
        }

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("শিক্ষাবর্ষ/সেশন পাওয়া যায়নি"));
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("শ্রেণি পাওয়া যায়নি"));

        Section defaultSection = null;
        if (defaultSectionId != null) {
            defaultSection = sectionRepository.findById(defaultSectionId)
                    .orElseThrow(() -> new NotFoundException("ডিফল্ট সেকশন পাওয়া যায়নি"));
        }

        List<ImportedStudentSummary> importedList = new ArrayList<>();
        List<StudentImportError> errorList = new ArrayList<>();
        Map<Long, Integer> currentMaxRolls = new HashMap<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            if (lastRowNum < 1) {
                return new StudentImportResult(0, 0, 0, List.of(), List.of(), "এক্সেলে কোনো শিক্ষার্থীর তথ্য পাওয়া যায়নি");
            }

            LocalDate admissionDate = DhakaTime.today();
            int currentYear = admissionDate.getYear();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                int displayRowNum = i + 1;
                String fullName = getCellValue(row.getCell(0));
                String genderStr = getCellValue(row.getCell(1));
                String dobStr = getCellValue(row.getCell(2));
                String phoneStr = getCellValue(row.getCell(3));
                String address = getCellValue(row.getCell(4));
                String guardianName = getCellValue(row.getCell(5));
                String guardianPhone = getCellValue(row.getCell(6));
                String relationStr = getCellValue(row.getCell(7));
                String sectionName = getCellValue(row.getCell(8));
                String rollStr = getCellValue(row.getCell(9));

                // ১. শিক্ষার্থীর নাম ভ্যালিডেশন
                if (fullName == null || fullName.isBlank()) {
                    errorList.add(new StudentImportError(displayRowNum, "অজ্ঞাত", "শিক্ষার্থীর নাম দেওয়া আবশ্যক"));
                    continue;
                }

                // ২. লিঙ্গ ভ্যালিডেশন
                Gender gender;
                try {
                    gender = Gender.valueOf(genderStr != null ? genderStr.trim().toUpperCase() : "");
                } catch (Exception e) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "লিঙ্গ 'MALE' বা 'FEMALE' হতে হবে"));
                    continue;
                }

                // ৩. জন্মতারিখ ভ্যালিডেশন
                LocalDate dob = parseDate(row.getCell(2), dobStr);
                if (dob == null) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "জন্মতারিখ সঠিক নয় (YYYY-MM-DD ফরম্যাট প্রয়োজন)"));
                    continue;
                }
                if (dob.isAfter(admissionDate)) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "জন্মতারিখ ভবিষ্যতের হতে পারবে না"));
                    continue;
                }

                // ৪. অভিভাবকের নাম ও ফোন
                if (guardianName == null || guardianName.isBlank()) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "অভিভাবকের নাম দেওয়া আবশ্যক"));
                    continue;
                }

                String normGuardianPhone;
                try {
                    normGuardianPhone = PhoneNumbers.normalize(guardianPhone);
                } catch (Exception e) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "অভিভাবকের ফোন নম্বরটি বৈধ নয়: " + guardianPhone));
                    continue;
                }

                // ৫. সম্পর্ক
                GuardianRelation relation = parseRelation(relationStr);

                // ৬. সেকশন নির্ধারণ
                Section section = defaultSection;
                if (sectionName != null && !sectionName.isBlank()) {
                    section = sectionRepository.findBySchoolClassIdAndNameIgnoreCase(classId, sectionName.trim())
                            .orElse(defaultSection);
                }
                if (section == null) {
                    errorList.add(new StudentImportError(displayRowNum, fullName, "শ্রেণির জন্য কোনো সেকশন পাওয়া যায়নি"));
                    continue;
                }

                // ৭. রোল নম্বর নির্ধারণ
                Long secId = section.getId();
                int nextRoll;
                if (rollStr != null && !rollStr.isBlank()) {
                    try {
                        nextRoll = (int) Double.parseDouble(rollStr.trim());
                        if (enrollmentRepository.existsBySessionIdAndSectionIdAndRollNumberAndStatus(
                                sessionId, secId, nextRoll, EnrollmentStatus.ACTIVE)) {
                            errorList.add(new StudentImportError(displayRowNum, fullName,
                                    section.getName() + " শাখায় রোল " + nextRoll + " ইতিমধ্যে বরাদ্দ রয়েছে"));
                            continue;
                        }
                    } catch (Exception e) {
                        errorList.add(new StudentImportError(displayRowNum, fullName, "রোল নম্বর সংখ্যায় হতে হবে"));
                        continue;
                    }
                } else {
                    int currentMax = currentMaxRolls.computeIfAbsent(secId,
                            id -> enrollmentRepository.maxRoll(sessionId, id, EnrollmentStatus.ACTIVE));
                    nextRoll = currentMax + 1;
                    currentMaxRolls.put(secId, nextRoll);
                }

                // ৮. অভিভাবক তৈরি বা লিঙ্ক
                Guardian guardian = guardianRepository.findByPhone(normGuardianPhone)
                        .orElseGet(() -> {
                            Guardian g = new Guardian();
                            g.setFullName(guardianName.trim());
                            g.setPhone(normGuardianPhone);
                            g.setAddress(address != null && !address.isBlank() ? address.trim() : null);
                            return guardianRepository.save(g);
                        });

                // ৯. শিক্ষার্থী সংরক্ষণ
                Student student = new Student();
                student.setStudentCode(codeGenerator.next(currentYear));
                student.setFullName(fullName.trim());
                student.setDateOfBirth(dob);
                student.setGender(gender);
                if (phoneStr != null && !phoneStr.isBlank()) {
                    try {
                        student.setPhone(PhoneNumbers.normalize(phoneStr));
                    } catch (Exception ignored) {
                    }
                }
                student.setAddress(address != null && !address.isBlank() ? address.trim() : null);
                student.setAdmissionDate(admissionDate);
                student.setStatus(StudentStatus.ACTIVE);
                studentRepository.save(student);

                // ১০. শিক্ষার্থী-অভিভাবক সম্পর্ক
                StudentGuardian sg = new StudentGuardian();
                sg.setStudent(student);
                sg.setGuardian(guardian);
                sg.setRelation(relation);
                sg.setPrimaryContact(true);
                studentGuardianRepository.save(sg);

                // ১১. সেশনে ভর্তি সম্পন্ন
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setSession(session);
                enrollment.setSchoolClass(schoolClass);
                enrollment.setSection(section);
                enrollment.setRollNumber(nextRoll);
                enrollment.setStartDate(admissionDate);
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);

                importedList.add(new ImportedStudentSummary(
                        student.getStudentCode(),
                        student.getFullName(),
                        nextRoll,
                        section.getName()
                ));
            }

            int success = importedList.size();
            int failed = errorList.size();
            int total = success + failed;

            auditService.log("STUDENT_BULK_IMPORTED", "STUDENT", null,
                    String.format("সেশন: %s, শ্রেণি: %s — মোট %d জন সফল, %d জন ব্যর্থ",
                            session.getName(), schoolClass.getName(), success, failed));

            return new StudentImportResult(
                    total,
                    success,
                    failed,
                    importedList,
                    errorList,
                    String.format("মোট %d জন প্রসেস করা হয়েছে। %d জন সফলভাবে ভর্তি হয়েছে এবং %d টি ত্রুটি পাওয়া গেছে।",
                            total, success, failed)
            );

        } catch (Exception e) {
            log.error("বাল্ক ইমপোর্টে মারাত্মক ত্রুটি", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "এক্সেল ফাইল রিড করতে সমস্যা হয়েছে: " + e.getMessage());
        }
    }

    /**
     * নির্দিষ্ট শ্রেণি/সেকশনের শিক্ষার্থীদের এক্সেল ফাইলে এক্সপোর্ট
     */
    @Transactional(readOnly = true)
    public byte[] exportStudents(Long sessionId, Long classId, Long sectionId) {
        List<Enrollment> enrollments;
        if (sectionId != null) {
            enrollments = enrollmentRepository.findBySessionIdAndSectionIdAndStatus(sessionId, sectionId, EnrollmentStatus.ACTIVE);
        } else {
            enrollments = enrollmentRepository.findInClassAndSession(sessionId, classId, EnrollmentStatus.ACTIVE);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("শিক্ষার্থী তালিকা");

            // হেডার স্টাইল
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(headerStyle);

            // সাধারণ ডাটা স্টাইল
            CellStyle dataStyle = workbook.createCellStyle();
            setBorders(dataStyle);

            String[] headers = {
                    "রোল",
                    "আইডি (Student Code)",
                    "শিক্ষার্থীর নাম",
                    "শাখা",
                    "লিঙ্গ",
                    "জন্মতারিখ",
                    "শিক্ষার্থীর ফোন",
                    "প্রধান অভিভাবকের নাম",
                    "অভিভাবকের ফোন",
                    "ঠিকানা"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < enrollments.size(); i++) {
                Enrollment e = enrollments.get(i);
                Student s = e.getStudent();
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(e.getRollNumber());
                row.createCell(1).setCellValue(s.getStudentCode());
                row.createCell(2).setCellValue(s.getFullName());
                row.createCell(3).setCellValue(e.getSection() != null ? e.getSection().getName() : "");
                row.createCell(4).setCellValue(s.getGender().name());
                row.createCell(5).setCellValue(s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "");
                row.createCell(6).setCellValue(s.getPhone() != null ? s.getPhone() : "");

                // অভিভাবক তথ্য
                List<StudentGuardian> guardians = studentGuardianRepository.findAllWithGuardian(s.getId());
                if (!guardians.isEmpty()) {
                    Guardian primary = guardians.get(0).getGuardian();
                    row.createCell(7).setCellValue(primary.getFullName());
                    row.createCell(8).setCellValue(primary.getPhone());
                } else {
                    row.createCell(7).setCellValue("");
                    row.createCell(8).setCellValue("");
                }

                row.createCell(9).setCellValue(s.getAddress() != null ? s.getAddress() : "");

                for (int c = 0; c < headers.length; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("শিক্ষার্থী এক্সপোর্ট তৈরিতে ত্রুটি", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "শিক্ষার্থী তালিকা এক্সপোর্ট করতে ব্যর্থ হয়েছে");
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            return new BigDecimal(String.valueOf(cell.getNumericCellValue())).stripTrailingZeros().toPlainString();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    private LocalDate parseDate(Cell cell, String str) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (str == null || str.isBlank()) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private GuardianRelation parseRelation(String str) {
        if (str == null || str.isBlank()) {
            return GuardianRelation.FATHER;
        }
        String upper = str.trim().toUpperCase();
        try {
            return GuardianRelation.valueOf(upper);
        } catch (Exception ignored) {
        }
        if (upper.contains("পিতা") || upper.contains("বাবা") || upper.contains("FATHER")) {
            return GuardianRelation.FATHER;
        }
        if (upper.contains("মাতা") || upper.contains("মা") || upper.contains("MOTHER")) {
            return GuardianRelation.MOTHER;
        }
        if (upper.contains("অভিভাবক") || upper.contains("GUARDIAN")) {
            return GuardianRelation.GUARDIAN;
        }
        return GuardianRelation.GUARDIAN;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValue(cell);
                if (val != null && !val.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }
}
