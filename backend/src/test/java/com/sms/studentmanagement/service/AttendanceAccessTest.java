package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.TeacherAssignmentRepository;
import com.sms.studentmanagement.repository.TeacherRepository;
import com.sms.studentmanagement.security.Actor;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceAccessTest {

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    TeacherAssignmentRepository assignmentRepository;

    AttendanceAccess access;

    final LocalDate day = LocalDate.of(2026, 11, 8);
    final Actor teacherActor = new Actor(10L, false);

    @BeforeEach
    void setUp() {
        access = new AttendanceAccess(teacherRepository, assignmentRepository);
    }

    private Teacher teacher(boolean active) {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setActive(active);
        return teacher;
    }

    @Test
    void adminNeedsNoAssignment() {
        assertDoesNotThrow(() -> access.requireSection(new Actor(1L, true), 7L, day, day));
        verifyNoInteractions(teacherRepository, assignmentRepository);
    }

    @Test
    void teacherWithAssignmentIsAllowed() {
        when(teacherRepository.findByUserId(10L)).thenReturn(Optional.of(teacher(true)));
        when(assignmentRepository.countCoverage(anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(1L);

        assertDoesNotThrow(() -> access.requireSection(teacherActor, 7L, day, day));
    }

    @Test
    void teacherWithoutAssignmentGets403() {
        when(teacherRepository.findByUserId(10L)).thenReturn(Optional.of(teacher(true)));
        when(assignmentRepository.countCoverage(anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);

        ApiException error = assertThrows(ApiException.class, () -> access.requireSection(teacherActor, 7L, day, day));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void inactiveTeacherGets403() {
        when(teacherRepository.findByUserId(10L)).thenReturn(Optional.of(teacher(false)));

        assertThrows(ApiException.class, () -> access.requireSection(teacherActor, 7L, day, day));
    }

    @Test
    void userWithoutTeacherProfileGets403() {
        when(teacherRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> access.requireSection(teacherActor, 7L, day, day));
    }
}