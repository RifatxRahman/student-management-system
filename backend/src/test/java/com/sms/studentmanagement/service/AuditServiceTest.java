package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.AuditLogResponse;
import com.sms.studentmanagement.entity.AuditLog;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.repository.AuditLogRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AuditLogRepository auditLogRepository;

    @Mock
    UserRepository userRepository;

    AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(auditLogRepository, userRepository);
    }

    @Test
    void log_capturesActionAndMasksDetails() {
        String details = "পাসওয়ার্ড পরিবর্তন password: mysecret123 এবং Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc";
        service.log("AUTH_PASSWORD_CHANGED", "USER", 5L, details, "SUCCESS", 45L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("AUTH_PASSWORD_CHANGED", saved.getAction());
        assertEquals("USER", saved.getEntityType());
        assertEquals(5L, saved.getEntityId());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals(45L, saved.getExecutionTimeMs());
        // Verify sensitive data masked
        assertNotNull(saved.getDetails());
        org.junit.jupiter.api.Assertions.assertFalse(saved.getDetails().contains("mysecret123"));
        org.junit.jupiter.api.Assertions.assertTrue(saved.getDetails().contains("***PROTECTED***"));
    }

    @Test
    void search_returnsPaginatedResponseWithUsernames() {
        AuditLog log1 = new AuditLog();
        log1.setId(101L);
        log1.setActorUserId(1L);
        log1.setAction("STUDENT_CREATED");
        log1.setEntityType("STUDENT");
        log1.setEntityId(10L);
        log1.setStatus("SUCCESS");
        log1.setDetails("নতুন শিক্ষার্থী ভর্তি");

        List<AuditLog> logList = new ArrayList<>();
        logList.add(log1);
        PageImpl<AuditLog> page = new PageImpl<>(logList, PageRequest.of(0, 10), 1);

        when(auditLogRepository.searchAdvanced(
                eq("STUDENT"), eq(10L), eq("STUDENT_CREATED"), eq("SUCCESS"),
                eq(null), eq(null), eq(null), any(PageRequest.class)
        )).thenReturn(page);

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findAllById(any())).thenReturn(users);

        PageResponse<AuditLogResponse> response = service.search(
                "STUDENT", 10L, "STUDENT_CREATED", "SUCCESS", null, null, null, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("admin", response.items().get(0).actorUsername());
        assertEquals("SUCCESS", response.items().get(0).status());
    }
}
