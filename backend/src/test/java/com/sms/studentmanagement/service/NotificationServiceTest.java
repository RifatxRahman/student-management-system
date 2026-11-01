package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.NotificationDtos.DispatchResult;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.entity.NotificationType;
import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.entity.StudentGuardian;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.NotificationLogRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import java.time.LocalDate;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    NotificationLogRepository repository;

    @Mock
    StudentGuardianRepository studentGuardianRepository;

    @Mock
    SmsSender smsSender;

    @Mock
    AuditService auditService;

    NotificationService service;

    final LocalDate date = LocalDate.of(2026, 11, 2);

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, studentGuardianRepository, smsSender, auditService);
    }

    private Student student() {
        Student s = new Student();
        s.setId(100L);
        s.setStudentCode("2026-00001");
        s.setFullName("Rahim Uddin");
        return s;
    }

    private Guardian guardian() {
        Guardian g = new Guardian();
        g.setId(7L);
        g.setFullName("Karim Uddin");
        g.setPhone("01712345678");
        return g;
    }

    private NotificationLog log(NotificationStatus status) {
        NotificationLog n = new NotificationLog();
        n.setId(1L);
        n.setStudent(student());
        n.setGuardian(guardian());
        n.setNotificationType(NotificationType.ABSENCE);
        n.setEventDate(date);
        n.setPhone("01712345678");
        n.setMessage("test message");
        n.setStatus(status);
        return n;
    }

    @Test
    void absenceIsQueuedAsPendingForPrimaryGuardian() {
        Student s = student();
        StudentGuardian link = new StudentGuardian();
        link.setStudent(s);
        link.setGuardian(guardian());
        link.setPrimaryContact(true);
        when(studentGuardianRepository.findAllWithGuardian(100L)).thenReturn(List.of(link));

        service.queueAbsence(s, date);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).save(captor.capture());
        NotificationLog saved = captor.getValue();
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals("01712345678", saved.getPhone());
        assertEquals(0, saved.getAttempts());
        assertEquals(date, saved.getEventDate());
        assertTrue(saved.getMessage().contains("Rahim Uddin"));
    }

    @Test
    void sameStudentSameDayIsNotQueuedTwice() {
        when(repository.existsByStudentIdAndNotificationTypeAndEventDate(100L, NotificationType.ABSENCE, date))
                .thenReturn(true);

        service.queueAbsence(student(), date);

        verify(repository, never()).save(any());
    }

    @Test
    void studentWithoutGuardianQueuesNothing() {
        when(studentGuardianRepository.findAllWithGuardian(100L)).thenReturn(List.of());

        service.queueAbsence(student(), date);

        verify(repository, never()).save(any());
    }

    @Test
    void cancelOnlyTouchesUnsentMessages() {
        service.cancelUnsentAbsence(100L, date);

        verify(repository).deleteByStudentIdAndNotificationTypeAndEventDateAndStatusIn(
                100L, NotificationType.ABSENCE, date,
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED));
    }

    @Test
    void successfulDispatchMarksSent() {
        NotificationLog n = log(NotificationStatus.PENDING);
        when(repository.findByStatusOrderByIdAsc(eq(NotificationStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(n));

        DispatchResult result = service.dispatchPending();

        assertEquals(1, result.sent());
        assertEquals(0, result.failed());
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertEquals(1, n.getAttempts());
        assertNotNull(n.getSentAt());
        verify(repository).save(n);
    }

    @Test
    void failedDispatchMarksFailedWithReason() {
        NotificationLog n = log(NotificationStatus.PENDING);
        when(repository.findByStatusOrderByIdAsc(eq(NotificationStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(n));
        doThrow(new RuntimeException("Gateway down")).when(smsSender).send(anyString(), anyString());

        DispatchResult result = service.dispatchPending();

        assertEquals(0, result.sent());
        assertEquals(1, result.failed());
        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals("Gateway down", n.getLastError());
        assertEquals(1, n.getAttempts());
    }

    @Test
    void failedMessageCanBeRetried() {
        NotificationLog n = log(NotificationStatus.FAILED);
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        service.retry(1L);

        assertEquals(NotificationStatus.PENDING, n.getStatus());
    }

    @Test
    void sentMessageCannotBeRetried() {
        NotificationLog n = log(NotificationStatus.SENT);
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(ApiException.class, () -> service.retry(1L));
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }
}