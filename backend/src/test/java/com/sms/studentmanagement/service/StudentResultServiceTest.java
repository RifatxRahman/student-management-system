package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.StudentResultDtos.StudentResultSummary;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.Exam;
import com.sms.studentmanagement.entity.ExamResult;
import com.sms.studentmanagement.entity.ExamType;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.ExamResultRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentResultServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    ExamResultRepository resultRepository;

    StudentResultService service;

    @BeforeEach
    void setUp() {
        service = new StudentResultService(studentRepository, resultRepository);
    }

    @Test
    void unknownStudentIsNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.history(99L));
    }

    @Test
    void failedResultHasNoPositionAndLinksAreFilled() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));

        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(5L);
        schoolClass.setName("Class 6");
        Exam exam = new Exam();
        exam.setId(7L);
        exam.setName("Final");
        exam.setExamType(ExamType.FINAL);
        exam.setSession(session);
        exam.setSchoolClass(schoolClass);
        Section section = new Section();
        section.setId(10L);
        section.setName("A");
        Enrollment enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setSection(section);
        enrollment.setRollNumber(4);

        ExamResult result = new ExamResult();
        result.setId(900L);
        result.setExam(exam);
        result.setEnrollment(enrollment);
        result.setTotalMarks(new BigDecimal("250.00"));
        result.setTotalFullMarks(new BigDecimal("600.00"));
        result.setAverageMarks(new BigDecimal("41.67"));
        result.setGpa(new BigDecimal("0.00"));
        result.setGrade("F");
        result.setPassed(false);
        when(resultRepository.findAllForStudent(1L)).thenReturn(List.of(result));

        List<StudentResultSummary> history = service.history(1L);

        assertEquals(1, history.size());
        StudentResultSummary summary = history.get(0);
        assertEquals(7L, summary.examId());
        assertEquals(10L, summary.sectionId());
        assertEquals(100L, summary.enrollmentId());
        assertEquals("Class 6", summary.className());
        assertEquals(4, summary.rollNumber());
        assertNull(summary.positionInClass());
    }
}