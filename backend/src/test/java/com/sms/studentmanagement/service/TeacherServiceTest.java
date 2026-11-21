package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.PeopleDtos.TeacherRequest;
import com.sms.studentmanagement.dto.PeopleDtos.TeacherResponse;
import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import com.sms.studentmanagement.repository.ClassSubjectRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.time.LocalDate;
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
class TeacherServiceTest {

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    SectionRepository sectionRepository;

    @Mock
    SubjectRepository subjectRepository;

    @Mock
    ClassSubjectRepository classSubjectRepository;

    @Mock
    AuditService auditService;

    TeacherService service;

    @BeforeEach
    void setUp() {
        service = new TeacherService(
                teacherRepository,
                assignmentRepository,
                userRepository,
                sessionRepository,
                sectionRepository,
                subjectRepository,
                classSubjectRepository,
                auditService);
    }

    @Test
    void create_success() {
        TeacherRequest request = new TeacherRequest(
                "T-2026-001",
                "  Rahim Uddin  ",
                "01711112233",
                "rahim@school.edu.bd",
                "Senior Teacher",
                "M.Sc in Physics",
                LocalDate.of(2022, 1, 15),
                "Dhaka, Bangladesh",
                null);

        when(teacherRepository.existsByEmployeeId("T-2026-001")).thenReturn(false);

        TeacherResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("T-2026-001", res.employeeId());
        assertEquals("Rahim Uddin", res.fullName());
        assertEquals("Senior Teacher", res.designation());
        assertEquals("M.Sc in Physics", res.qualification());
        verify(auditService).log(eq("TEACHER_CREATED"), eq("TEACHER"), any(), eq("Rahim Uddin"));
    }

    @Test
    void create_duplicateEmployeeId_throwsConflict() {
        TeacherRequest request = new TeacherRequest(
                "T-DUP-01",
                "Karim Hossain",
                "01722223344",
                null,
                "Assistant Teacher",
                null,
                null,
                null,
                null);

        when(teacherRepository.existsByEmployeeId("T-DUP-01")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void update_duplicateEmployeeId_throwsConflict() {
        Teacher existing = new Teacher();
        existing.setId(10L);
        existing.setEmployeeId("T-OLD");
        existing.setFullName("Existing Teacher");

        TeacherRequest request = new TeacherRequest(
                "T-NEW-TAKEN",
                "Existing Teacher",
                "01722223344",
                null,
                "Assistant Teacher",
                null,
                null,
                null,
                null);

        when(teacherRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(teacherRepository.existsByEmployeeId("T-NEW-TAKEN")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.update(10L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void list_mapsAllEntitiesUsingIndexLoop() {
        Teacher t1 = new Teacher();
        t1.setId(1L);
        t1.setFullName("Teacher One");

        Teacher t2 = new Teacher();
        t2.setId(2L);
        t2.setFullName("Teacher Two");

        List<Teacher> list = new ArrayList<>();
        list.add(t1);
        list.add(t2);

        when(teacherRepository.findAllWithUser()).thenReturn(list);

        List<TeacherResponse> result = service.list();
        assertEquals(2, result.size());
        assertEquals("Teacher One", result.get(0).fullName());
        assertEquals("Teacher Two", result.get(1).fullName());
    }
}
