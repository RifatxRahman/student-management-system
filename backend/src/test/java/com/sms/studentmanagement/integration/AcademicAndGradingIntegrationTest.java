package com.sms.studentmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.MarkDtos.MarkEntryRequest;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksRequest;
import com.sms.studentmanagement.dto.UserResponse;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.MarkRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.service.AttendanceAccess;
import com.sms.studentmanagement.service.AuditService;
import com.sms.studentmanagement.service.MarksService;
import com.sms.studentmanagement.service.ResultService;
import com.sms.studentmanagement.util.GradeBand;
import com.sms.studentmanagement.util.ResultCalculator;
import com.sms.studentmanagement.util.ResultCalculator.Result;
import com.sms.studentmanagement.util.ResultCalculator.SubjectInput;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
class AcademicAndGradingIntegrationTest {

    @Mock
    ExamRepository examRepository;

    @Mock
    ExamSubjectRepository examSubjectRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    MarkRepository markRepository;

    @Mock
    AttendanceAccess access;

    @Mock
    ResultService resultService;

    @Mock
    AuditService auditService;

    MarksService marksService;

    Exam exam;
    ExamSubject examSubject;
    Section section;
    Enrollment enrollment;
    Actor adminActor;

    List<GradeBand> standardGradeBands;

    @BeforeEach
    void setUp() {
        marksService = new MarksService(
                examRepository,
                examSubjectRepository,
                sectionRepository,
                assignmentRepository,
                enrollmentRepository,
                markRepository,
                access,
                resultService,
                auditService
        );

        adminActor = new Actor(1L, true);

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        SchoolClass sc = new SchoolClass();
        sc.setId(10L);
        sc.setName("Class 10");

        section = new Section();
        section.setId(101L);
        section.setName("Section A");
        section.setSchoolClass(sc);

        exam = new Exam();
        exam.setId(201L);
        exam.setName("বার্ষিক পরীক্ষা ২০২৬");
        exam.setSession(session);
        exam.setSchoolClass(sc);
        exam.setStatus(com.sms.studentmanagement.entity.ExamStatus.DRAFT);

        Subject subject = new Subject();
        subject.setId(301L);
        subject.setName("গণিত");
        subject.setCode("MATH");

        examSubject = new ExamSubject();
        examSubject.setId(401L);
        examSubject.setExam(exam);
        examSubject.setSubject(subject);
        examSubject.setFullMarks(new BigDecimal("100.00"));
        examSubject.setPassMarks(new BigDecimal("33.00"));

        Student student = new Student();
        student.setId(501L);
        student.setStudentCode("2026-00501");
        student.setFullName("রাকিব হাসান");

        enrollment = new Enrollment();
        enrollment.setId(601L);
        enrollment.setStudent(student);
        enrollment.setSchoolClass(sc);
        enrollment.setSection(section);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        // জাতীয় গ্রেডিং স্কেল তৈরি
        standardGradeBands = new ArrayList<>();
        standardGradeBands.add(new GradeBand(new BigDecimal("80.00"), "A+", new BigDecimal("5.00")));
        standardGradeBands.add(new GradeBand(new BigDecimal("70.00"), "A", new BigDecimal("4.00")));
        standardGradeBands.add(new GradeBand(new BigDecimal("60.00"), "A-", new BigDecimal("3.50")));
        standardGradeBands.add(new GradeBand(new BigDecimal("50.00"), "B", new BigDecimal("3.00")));
        standardGradeBands.add(new GradeBand(new BigDecimal("40.00"), "C", new BigDecimal("2.00")));
        standardGradeBands.add(new GradeBand(new BigDecimal("33.00"), "D", new BigDecimal("1.00")));
        standardGradeBands.add(new GradeBand(new BigDecimal("0.00"), "F", new BigDecimal("0.00")));
    }

    @Test
    void marksEntry_exceedingFullMarks_isRejectedWithBadRequest() {
        when(examRepository.findWithRefs(201L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByExamIdAndSubjectId(201L, 301L)).thenReturn(Optional.of(examSubject));
        when(sectionRepository.findById(101L)).thenReturn(Optional.of(section));

        List<Enrollment> roster = new ArrayList<>();
        roster.add(enrollment);
        when(enrollmentRepository.findInClassAndSession(1L, 10L, EnrollmentStatus.ACTIVE))
                .thenReturn(roster);

        // ফুল মার্কস ১০০ কিন্তু ১০৫ নম্বর দেওয়ার চেষ্টা
        List<MarkEntryRequest> entries = new ArrayList<>();
        entries.add(new MarkEntryRequest(601L, new BigDecimal("105.00"), false));
        SaveMarksRequest request = new SaveMarksRequest(201L, 101L, 301L, entries, null);

        ApiException ex = assertThrows(ApiException.class, () -> marksService.save(adminActor, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("ফুল মার্কস"));
    }

    @Test
    void marksEntry_negativeMarks_isRejectedWithBadRequest() {
        when(examRepository.findWithRefs(201L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByExamIdAndSubjectId(201L, 301L)).thenReturn(Optional.of(examSubject));
        when(sectionRepository.findById(101L)).thenReturn(Optional.of(section));

        List<Enrollment> roster = new ArrayList<>();
        roster.add(enrollment);
        when(enrollmentRepository.findInClassAndSession(1L, 10L, EnrollmentStatus.ACTIVE))
                .thenReturn(roster);

        // ঋণাত্মক নম্বর দেওয়ার চেষ্টা
        List<MarkEntryRequest> entries = new ArrayList<>();
        entries.add(new MarkEntryRequest(601L, new BigDecimal("-10.00"), false));
        SaveMarksRequest request = new SaveMarksRequest(201L, 101L, 301L, entries, null);

        ApiException ex = assertThrows(ApiException.class, () -> marksService.save(adminActor, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("ঋণাত্মক"));
    }

    @Test
    void resultCalculation_failingOneCompulsorySubject_forcesGpaZeroAndFail() {
        List<SubjectInput> inputs = new ArrayList<>();
        // বাংলা: ১০০ (A+, 5.0)
        inputs.add(new SubjectInput(1L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("100")));
        // ইংরেজি: ১০০ (A+, 5.0)
        inputs.add(new SubjectInput(2L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("100")));
        // গণিত: ২৫ (Pass Mark 33, তাই ফেল - F)
        inputs.add(new SubjectInput(3L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("25")));

        Result result = ResultCalculator.calculate(inputs, standardGradeBands, new BigDecimal("2.00"), new BigDecimal("5.00"));

        assertNotNull(result);
        assertFalse(result.passed(), "আবশ্যিক বিষয়ে ফেল করায় পাস হতে পারে না!");
        assertEquals("F", result.grade(), "একটি বিষয়ে ফেল করলে চূড়ান্ত গ্রেড F হতে হবে!");
        assertEquals(new BigDecimal("0.00"), result.gpa(), "ফেল করলে জিপিএ ০.০০ হতে হবে!");
    }

    @Test
    void resultCalculation_allSubjectsPassed_calculatesAccurateGpaAndGrade() {
        List<SubjectInput> inputs = new ArrayList<>();
        // বাংলা: ৮৫ (A+, 5.0)
        inputs.add(new SubjectInput(1L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("85")));
        // ইংরেজি: ৭২ (A, 4.0)
        inputs.add(new SubjectInput(2L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("72")));
        // গণিত: ৮৮ (A+, 5.0)
        inputs.add(new SubjectInput(3L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("88")));

        // গড় জিপিএ: (5.0 + 4.0 + 5.0) / 3 = 14.0 / 3 = 4.67 (A)
        Result result = ResultCalculator.calculate(inputs, standardGradeBands, new BigDecimal("2.00"), new BigDecimal("5.00"));

        assertNotNull(result);
        assertTrue(result.passed());
        assertEquals(new BigDecimal("4.67"), result.gpa());
        assertEquals("A", result.grade());
    }

    @Test
    void resultCalculation_optionalSubjectBonus_addsToTotalGpaProperly() {
        List<SubjectInput> inputs = new ArrayList<>();
        // আবশ্যিক ১: ৭৫ (A, 4.0)
        inputs.add(new SubjectInput(1L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("75")));
        // আবশ্যিক ২: ৭৫ (A, 4.0)
        inputs.add(new SubjectInput(2L, new BigDecimal("100"), new BigDecimal("33"), false, true, false, new BigDecimal("75")));
        // ঐচ্ছিক বিষয় (উচ্চতর গণিত): ৯৫ (A+, 5.0)। বেস ২.০০ এর বেশি পয়েন্ট = (৫.০ - ২.০) = ৩.০ বোনাস পয়েন্ট
        inputs.add(new SubjectInput(3L, new BigDecimal("100"), new BigDecimal("33"), true, true, false, new BigDecimal("95")));

        // আবশ্যিক পয়েন্ট সমষ্টি = 8.0, বোনাস = 3.0 -> সর্বমোট = 11.0 / 2 (আবশ্যিক বিষয় সংখ্যা) = 5.50 -> ম্যাক্স ক্যাপ 5.00
        Result result = ResultCalculator.calculate(inputs, standardGradeBands, new BigDecimal("2.00"), new BigDecimal("5.00"));

        assertNotNull(result);
        assertTrue(result.passed());
        assertEquals(new BigDecimal("5.00"), result.gpa(), "ঐচ্ছিক বিষয়ের বোনাস পয়েন্ট যোগ হয়ে জিপিএ ৫.০০ হতে হবে!");
        assertEquals("A+", result.grade());
    }
}
