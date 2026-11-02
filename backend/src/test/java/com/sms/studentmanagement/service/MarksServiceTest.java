package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.MarkDtos.MarkEntryRequest;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksRequest;
import com.sms.studentmanagement.dto.MarkDtos.SaveMarksResult;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Exam;
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
import com.sms.studentmanagement.repository.ExamSubjectRepository;
import com.sms.studentmanagement.repository.MarkRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.security.Actor;
import java.math.BigDecimal;
import java.util.List;
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
class MarksServiceTest {

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

    MarksService service;

    Exam exam;
    ExamSubject examSubject;
    Section section;
    SchoolClass schoolClass;
    Enrollment enrollment;

    final Actor admin = new Actor(1L, true);
    final Actor teacher = new Actor(10L, false);

    @BeforeEach
    void setUp() {
        service = new MarksService(
                examRepository, examSubjectRepository, sectionRepository, assignmentRepository,
                enrollmentRepository, markRepository, access, resultService, auditService);

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Class 6");
        section = new Section();
        section.setId(10L);
        section.setName("A");
        section.setSchoolClass(schoolClass);

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
        examSubject = new ExamSubject();
        examSubject.setId(70L);
        examSubject.setExam(exam);
        examSubject.setSubject(subject);
        examSubject.setFullMarks(new BigDecimal("100"));
        examSubject.setPassMarks(new BigDecimal("33"));

        Student student = new Student();
        student.setId(100L);
        student.setStudentCode("2026-00001");
        student.setFullName("Rahim");
        enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setStudent(student);
        enrollment.setSection(section);
        enrollment.setRollNumber(1);

        when(examRepository.findWithRefs(7L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByExamIdAndSubjectId(7L, 9L)).thenReturn(Optional.of(examSubject));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findInClassAndSession(1L, 5L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));

        Teacher teacherProfile = new Teacher();
        teacherProfile.setId(3L);
        teacherProfile.setActive(true);
        when(access.activeTeacher(teacher)).thenReturn(teacherProfile);
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(3L, 1L, 10L, 9L))
                .thenReturn(true);
    }

    private MarkEntryRequest entry(String marks) {
        return new MarkEntryRequest(100L, new BigDecimal(marks), false);
    }

    private SaveMarksRequest request(String reason, MarkEntryRequest... entries) {
        return new SaveMarksRequest(7L, 10L, 9L, List.of(entries), reason);
    }

    private Mark existingMark(String marks) {
        Mark mark = new Mark();
        mark.setId(500L);
        mark.setExamSubject(examSubject);
        mark.setEnrollment(enrollment);
        mark.setMarksObtained(new BigDecimal(marks));
        when(markRepository.findForSectionAndExamSubject(70L, 10L)).thenReturn(List.of(mark));
        return mark;
    }

    // ---------- মালিকানা-যাচাই (Negative Test) ----------
    @Test
    void assignedTeacherCanSaveMarks() {
        SaveMarksResult result = service.save(teacher, request(null, entry("85")));

        assertEquals(1, result.created());
        verify(markRepository).saveAll(any());
    }

    @Test
    void teacherWithoutAssignmentIsForbidden() {
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(3L, 1L, 10L, 9L))
                .thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.save(teacher, request(null, entry("85"))));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(markRepository, never()).saveAll(any());
    }

    @Test
    void teacherWithoutAssignmentCannotEvenSeeTheSheet() {
        when(assignmentRepository.existsByTeacherIdAndSessionIdAndSectionIdAndSubjectId(3L, 1L, 10L, 9L))
                .thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.sheet(teacher, 7L, 10L, 9L));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void sectionOfAnotherClassIsRejected() {
        SchoolClass other = new SchoolClass();
        other.setId(6L);
        section.setSchoolClass(other);

        ApiException error = assertThrows(ApiException.class, () -> service.save(admin, request(null, entry("85"))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    // ---------- নম্বরের নিয়ম ----------
    @Test
    void fullMarksItselfIsAccepted() {
        assertEquals(1, service.save(admin, request(null, entry("100"))).created());
    }

    @Test
    void marksAboveFullMarksAreRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.save(admin, request(null, entry("100.01"))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(markRepository, never()).saveAll(any());
    }

    @Test
    void negativeMarksAreRejected() {
        assertThrows(ApiException.class, () -> service.save(admin, request(null, entry("-1"))));
    }

    @Test
    void absentWithMarksIsRejected() {
        assertThrows(ApiException.class, () -> service.save(admin,
                request(null, new MarkEntryRequest(100L, new BigDecimal("50"), true))));
    }

    @Test
    void missingMarksWithoutAbsentIsRejected() {
        assertThrows(ApiException.class, () -> service.save(admin,
                request(null, new MarkEntryRequest(100L, null, false))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void absentIsSavedWithoutMarks() {
        service.save(admin, request(null, new MarkEntryRequest(100L, null, true)));

        ArgumentCaptor<List<Mark>> captor = ArgumentCaptor.forClass(List.class);
        verify(markRepository).saveAll(captor.capture());
        Mark saved = captor.getValue().get(0);
        assertTrue(saved.isAbsent());
        assertNull(saved.getMarksObtained());
        assertEquals(1L, saved.getEnteredBy());
    }

    @Test
    void studentOutsideTheSectionRosterIsRejected() {
        assertThrows(ApiException.class, () -> service.save(admin,
                request(null, new MarkEntryRequest(999L, new BigDecimal("50"), false))));
    }

    @Test
    void sameStudentTwiceIsRejected() {
        assertThrows(ApiException.class, () -> service.save(admin, request(null, entry("50"), entry("60"))));
    }

    // ---------- প্রকাশের পর Lock ----------
    @Test
    void afterPublishTeacherCannotChangeMarks() {
        exam.setStatus(ExamStatus.PUBLISHED);
        existingMark("80");

        ApiException error = assertThrows(ApiException.class, () -> service.save(teacher, request("ভুল ছিল", entry("85"))));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(resultService, never()).recalculate(any());
    }

    @Test
    void afterPublishAdminNeedsAReason() {
        exam.setStatus(ExamStatus.PUBLISHED);
        existingMark("80");

        ApiException error = assertThrows(ApiException.class, () -> service.save(admin, request(null, entry("85"))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertThrows(ApiException.class, () -> service.save(admin, request("ভুল", entry("85"))));
        verify(resultService, never()).recalculate(any());
        verify(auditService, never()).log(anyString(), anyString(), any(), anyString());
    }

    @Test
    void afterPublishAdminCorrectionIsAuditedAndResultsAreRecalculated() {
        exam.setStatus(ExamStatus.PUBLISHED);
        Mark mark = existingMark("80");

        SaveMarksResult result = service.save(admin, request("যোগ করতে ভুল হয়েছিল", entry("85")));

        assertEquals(1, result.changed());
        assertEquals(0, new BigDecimal("85").compareTo(mark.getMarksObtained()));
        verify(auditService).log(eq("MARKS_CORRECTED"), eq("EXAM"), eq(7L), org.mockito.ArgumentMatchers.contains("কারণ"));
        verify(resultService).recalculate(exam);
    }

    @Test
    void afterPublishSameMarksNeedNoReasonAndNoRecalculation() {
        exam.setStatus(ExamStatus.PUBLISHED);
        existingMark("80");

        SaveMarksResult result = service.save(admin, request(null, entry("80")));

        assertEquals(1, result.unchanged());
        verify(resultService, never()).recalculate(any());
    }

    @Test
    void beforePublishChangeNeedsNoReasonAndNoRecalculation() {
        existingMark("80");

        SaveMarksResult result = service.save(teacher, request(null, entry("85")));

        assertEquals(1, result.changed());
        verify(resultService, never()).recalculate(any());
        verify(auditService).log(eq("MARKS_SAVED"), eq("EXAM"), eq(7L), anyString());
    }
}