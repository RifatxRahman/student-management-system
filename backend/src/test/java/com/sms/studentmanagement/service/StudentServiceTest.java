package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.EnrollmentRepository;
import com.sms.studentmanagement.repository.GuardianRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.repository.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    GuardianRepository guardianRepository;

    @Mock
    StudentGuardianRepository studentGuardianRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    StudentCodeGenerator codeGenerator;

    @Mock
    AuditService auditService;

    StudentService service;
    Student student;

    @BeforeEach
    void setUp() {
        service = new StudentService(
                studentRepository, guardianRepository, studentGuardianRepository,
                enrollmentRepository, sessionRepository, codeGenerator, auditService);

        student = new Student();
        student.setId(1L);
        student.setStudentCode("2026-00001");
        student.setFullName("Rahim");
        student.setStatus(StudentStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
    }

    @Test
    void cannotDeleteStudentWithActiveEnrollment() {
        when(enrollmentRepository.existsByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThrows(ApiException.class, () -> service.softDelete(1L));
        assertNull(student.getDeletedAt());
    }

    @Test
    void canDeleteStudentWithoutActiveEnrollment() {
        when(enrollmentRepository.existsByStudentIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(false);

        service.softDelete(1L);

        assertNotNull(student.getDeletedAt());
    }

    @Test
    void graduatingClosesActiveEnrollments() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.of(2020, 1, 1));
        when(enrollmentRepository.findByStudentAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));

        service.changeStatus(1L, StudentStatus.GRADUATED);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
        assertNotNull(enrollment.getEndDate());
    }
}