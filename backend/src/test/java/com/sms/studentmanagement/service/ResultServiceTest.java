package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.config.ResultProperties;
import com.sms.studentmanagement.dto.ResultDtos.PublishResult;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamResult;
import com.sms.studentmanagement.entity.ExamStatus;
import com.sms.studentmanagement.entity.ExamSubject;
import com.sms.studentmanagement.entity.Mark;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.ExamRepository;
import com.sms.studentmanagement.repository.ExamResultRepository;
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.MarkRepository;
import com.sms.studentmanagement.repository.ResultSubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import com.sms.studentmanagement.util.GradeBand;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultServiceTest {

    @Mock
    ExamRepository examRepository;

    @Mock
    ExamSubjectRepository examSubjectRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    MarkRepository markRepository;

    @Mock
    ExamResultRepository resultRepository;

    @Mock
    ResultSubjectRepository resultSubjectRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    @Mock
    GradeScaleService gradeScaleService;

    @Mock
    AttendanceAccess access;

    @Mock
    AuditService auditService;

    ResultService service;

    Exam exam;
    ExamSubject math;
    List<Mark> marks;
    final Actor admin = new Actor(1L, true);
    final Actor teacher = new Actor(10L, false);

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        service = new ResultService(
                examRepository, examSubjectRepository, enrollmentRepository, markRepository, resultRepository,
                resultSubjectRepository, assignmentRepository, gradeScaleService, new ResultProperties(), access,
                auditService);

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Class 6");
        exam = new Exam();
        exam.setId(7L);
        exam.setName("Midterm");
        exam.setSession(session);
        exam.setSchoolClass(schoolClass);
        exam.setStatus(ExamStatus.DRAFT);

        Subject subject = new Subject();
        subject.setId(9L);
        subject.setName("Math");
        subject.setCode("MATH");
        math = new ExamSubject();
        math.setId(70L);
        math.setExam(exam);
        math.setSubject(subject);
        math.setFullMarks(bd("100"));
        math.setPassMarks(bd("33"));

        Section sectionA = section(10L, "A", schoolClass);
        Section sectionB = section(11L, "B", schoolClass);
        Enrollment e100 = enrollment(100L, 1, sectionA);
        Enrollment e101 = enrollment(101L, 2, sectionA);
        Enrollment e102 = enrollment(102L, 1, sectionB);
        Enrollment e103 = enrollment(103L, 2, sectionB);

        marks = new ArrayList<>(List.of(
                mark(e100, "90"), mark(e101, "70"), mark(e102, "90"), mark(e103, "20")));

        when(examRepository.findWithRefs(7L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findAllWithSubject(7L)).thenReturn(List.of(math));
        when(enrollmentRepository.findInClassAndSession(1L, 5L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(e100, e101, e102, e103));
        when(markRepository.findAllForExam(7L)).thenReturn(marks);
        when(gradeScaleService.currentBands()).thenReturn(List.of(
                new GradeBand(bd("80"), "A+", bd("5.00")), new GradeBand(bd("70"), "A", bd("4.00")),
                new GradeBand(bd("60"), "A-", bd("3.50")), new GradeBand(bd("50"), "B", bd("3.00")),
                new GradeBand(bd("40"), "C", bd("2.00")), new GradeBand(bd("33"), "D", bd("1.00")),
                new GradeBand(bd("0"), "F", bd("0.00"))));
    }

    private Section section(long id, String name, SchoolClass schoolClass) {
        Section section = new Section();
        section.setId(id);
        section.setName(name);
        section.setSchoolClass(schoolClass);
        return section;
    }

    private Enrollment enrollment(long id, int roll, Section section) {
        Student student = new Student();
        student.setId(id);
        student.setStudentCode("2026-0000" + id);
        student.setFullName("Student " + id);
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setRollNumber(roll);
        return enrollment;
    }

    private Mark mark(Enrollment enrollment, String value) {
        Mark mark = new Mark();
        mark.setExamSubject(math);
        mark.setEnrollment(enrollment);
        mark.setMarksObtained(bd(value));
        return mark;
    }

    // ---------- প্রকাশ ----------
    @Test
    void publishStoresResultsWithClassAndSectionPositions() {
        PublishResult result = service.publish(7L);

        assertEquals(4, result.students());
        assertEquals(3, result.passed());
        assertEquals(1, result.failed());
        assertEquals(ExamStatus.PUBLISHED, exam.getStatus());
        assertNotNull(exam.getPublishedAt());

        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(resultRepository, times(4)).save(captor.capture());
        Map<Long, ExamResult> byEnrollment = new HashMap<>();
        for (ExamResult r : captor.getAllValues()) {
            byEnrollment.put(r.getEnrollment().getId(), r);
        }

        // ১০০ (সেকশন A, ৯০) ও ১০২ (সেকশন B, ৯০): GPA ও মোট সমান → ক্লাসে দুজনেই ১ নম্বরে, ১০১ তিন নম্বরে
        assertEquals(1, byEnrollment.get(100L).getPositionInClass());
        assertEquals(1, byEnrollment.get(102L).getPositionInClass());
        assertEquals(3, byEnrollment.get(101L).getPositionInClass());
        assertEquals(1, byEnrollment.get(100L).getPositionInSection());
        assertEquals(2, byEnrollment.get(101L).getPositionInSection());
        assertEquals(1, byEnrollment.get(102L).getPositionInSection());
        assertEquals(0, bd("5.00").compareTo(byEnrollment.get(100L).getGpa()));
        assertEquals("A+", byEnrollment.get(100L).getGrade());

        // ফেল করা শিক্ষার্থীর কোনো অবস্থান নেই
        ExamResult failed = byEnrollment.get(103L);
        assertEquals(false, failed.isPassed());
        assertNull(failed.getPositionInClass());
        assertNull(failed.getPositionInSection());
        assertEquals(0, bd("0.00").compareTo(failed.getGpa()));
    }

    @Test
    void publishWithMissingMarksIsRejectedAndNothingIsSaved() {
        marks.remove(3);

        ApiException error = assertThrows(ApiException.class, () -> service.publish(7L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertTrue(error.getMessage().contains("Student 103"));
        verify(resultRepository, never()).save(any());
        assertEquals(ExamStatus.DRAFT, exam.getStatus());
    }

    @Test
    void publishingTwiceIsRejected() {
        exam.setStatus(ExamStatus.PUBLISHED);

        ApiException error = assertThrows(ApiException.class, () -> service.publish(7L));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void examWithoutCompulsorySubjectCannotBePublished() {
        math.setOptional(true);

        ApiException error = assertThrows(ApiException.class, () -> service.publish(7L));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void absentInCompulsorySubjectFailsTheStudent() {
        Mark absent = marks.get(0);
        absent.setAbsent(true);
        absent.setMarksObtained(null);

        PublishResult result = service.publish(7L);

        assertEquals(2, result.failed());
    }

    @Test
    void previewShowsMissingMarksWithoutSavingAnything() {
        marks.remove(3);

        var preview = service.preview(7L);

        assertEquals(4, preview.studentCount());
        assertEquals(3, preview.completeCount());
        assertEquals(1, preview.missingTotal());
        assertEquals("Student 103", preview.missing().get(0).studentName());
        verify(resultRepository, never()).save(any());
    }

    // ---------- প্রকাশিত ফল দেখার অনুমতি ----------
    @Test
    void resultsAreHiddenUntilPublished() {
        ApiException error = assertThrows(ApiException.class, () -> service.results(admin, 7L, null));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void teacherMustChooseASection() {
        exam.setStatus(ExamStatus.PUBLISHED);

        ApiException error = assertThrows(ApiException.class, () -> service.results(teacher, 7L, null));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void teacherCannotSeeResultsOfASectionTheyDoNotTeach() {
        exam.setStatus(ExamStatus.PUBLISHED);
        Teacher profile = new Teacher();
        profile.setId(3L);
        profile.setActive(true);
        when(access.activeTeacher(teacher)).thenReturn(profile);
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionId(3L, 1L, 10L)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.results(teacher, 7L, 10L));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }
}