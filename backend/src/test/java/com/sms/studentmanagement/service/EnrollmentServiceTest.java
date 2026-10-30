package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SchoolClassRepository classRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    AuditService auditService;

    EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
                studentRepository, sessionRepository, classRepository,
                sectionRepository, enrollmentRepository, auditService);
    }

    private Student student(StudentStatus status) {
        Student student = new Student();
        student.setId(1L);
        student.setStatus(status);
        return student;
    }

    private SchoolClass schoolClass(long id, boolean active) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setName("Class " + id);
        schoolClass.setActive(active);
        return schoolClass;
    }

    private Section section(long id, SchoolClass schoolClass, boolean active) {
        Section section = new Section();
        section.setId(id);
        section.setName("A");
        section.setSchoolClass(schoolClass);
        section.setActive(active);
        return section;
    }

    private AcademicSession session() {
        AcademicSession session = new AcademicSession();
        session.setId(5L);
        session.setName("2026");
        session.setStartDate(LocalDate.of(2026, 1, 1));
        session.setEndDate(LocalDate.of(2026, 12, 31));
        return session;
    }

    @Test
    void rejectsInactiveStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(StudentStatus.INACTIVE)));

        assertThrows(ApiException.class, () -> service.enroll(1L, 5L, 1L, 10L, null, null));
    }

    @Test
    void rejectsSectionFromAnotherClass() {
        SchoolClass class1 = schoolClass(1L, true);
        SchoolClass class2 = schoolClass(2L, true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(StudentStatus.ACTIVE)));
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session()));
        when(classRepository.findById(1L)).thenReturn(Optional.of(class1));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section(10L, class2, true)));

        assertThrows(ApiException.class, () -> service.enroll(1L, 5L, 1L, 10L, null, null));
    }

    @Test
    void rejectsInactiveClass() {
        SchoolClass class1 = schoolClass(1L, false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(StudentStatus.ACTIVE)));
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session()));
        when(classRepository.findById(1L)).thenReturn(Optional.of(class1));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section(10L, class1, true)));

        assertThrows(ApiException.class, () -> service.enroll(1L, 5L, 1L, 10L, null, null));
    }
}