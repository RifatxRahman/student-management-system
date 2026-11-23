package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventRequest;
import com.sms.studentmanagement.dto.AcademicEventDtos.AcademicEventResponse;
import com.sms.studentmanagement.entity.AcademicEvent;
import com.sms.studentmanagement.entity.AcademicEventType;
import com.sms.studentmanagement.entity.AcademicSession;
import com.sms.studentmanagement.entity.EventAudience;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicEventRepository;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
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
class AcademicEventServiceTest {

    @Mock
    AcademicEventRepository eventRepository;

    @Mock
    AcademicSessionRepository sessionRepository;

    @Mock
    AuditService auditService;

    AcademicEventService service;

    @BeforeEach
    void setUp() {
        service = new AcademicEventService(eventRepository, sessionRepository, auditService);
    }

    @Test
    void create_success() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        AcademicEventRequest req = new AcademicEventRequest(
                1L,
                "Annual Sports Day",
                AcademicEventType.SPORTS,
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 21),
                false,
                "Annual sports competition for all classes",
                EventAudience.ALL);

        AcademicEventResponse res = service.create(req);

        assertNotNull(res);
        assertEquals("Annual Sports Day", res.title());
        assertEquals(AcademicEventType.SPORTS, res.eventType());
        verify(eventRepository).save(any(AcademicEvent.class));
        verify(auditService).log(any(), any(), any(), any());
    }

    @Test
    void create_invalidDates_throwsBadRequest() {
        AcademicEventRequest req = new AcademicEventRequest(
                1L,
                "Invalid Event",
                AcademicEventType.OTHER,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 10), // End before start
                false,
                null,
                EventAudience.ALL);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void list_mapsAllEntitiesUsingIndexLoop() {
        AcademicSession session = new AcademicSession();
        session.setId(1L);
        session.setName("2026");

        AcademicEvent e1 = new AcademicEvent();
        e1.setId(10L);
        e1.setSession(session);
        e1.setTitle("Cultural Program");
        e1.setEventType(AcademicEventType.CULTURAL);
        e1.setStartDate(LocalDate.of(2026, 3, 26));
        e1.setEndDate(LocalDate.of(2026, 3, 26));

        List<AcademicEvent> list = new ArrayList<>();
        list.add(e1);

        when(eventRepository.findBySessionIdWithSession(1L)).thenReturn(list);

        List<AcademicEventResponse> result = service.list(1L, null, null);
        assertEquals(1, result.size());
        assertEquals("Cultural Program", result.get(0).title());
    }
}
