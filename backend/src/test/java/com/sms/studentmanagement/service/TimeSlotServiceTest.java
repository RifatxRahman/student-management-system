package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.RoutineDtos.TimeSlotRequest;
import com.sms.studentmanagement.entity.TimeSlot;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.TimeSlotRepository;
import com.sms.studentmanagement.repository.TimetableRepository;
import java.time.LocalTime;
import java.util.List;
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
class TimeSlotServiceTest {

    @Mock
    TimeSlotRepository slotRepository;

    @Mock
    TimetableRepository timetableRepository;

    @Mock
    AuditService auditService;

    TimeSlotService service;

    @BeforeEach
    void setUp() {
        service = new TimeSlotService(slotRepository, timetableRepository, auditService);

        TimeSlot first = new TimeSlot();
        first.setId(1L);
        first.setName("1st");
        first.setStartTime(LocalTime.of(9, 0));
        first.setEndTime(LocalTime.of(9, 45));
        when(slotRepository.findAllByOrderByStartTime()).thenReturn(List.of(first));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(first));
    }

    private TimeSlotRequest request(String name, String start, String end) {
        return new TimeSlotRequest(name, LocalTime.parse(start), LocalTime.parse(end), null);
    }

    @Test
    void slotTouchingAnotherIsAllowed() {
        service.create(request("2nd", "09:45", "10:30"));

        verify(slotRepository).save(any(TimeSlot.class));
    }

    @Test
    void overlappingSlotIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.create(request("Mid", "09:30", "10:15")));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(slotRepository, never()).save(any());
    }

    @Test
    void duplicateNameIsRejected() {
        when(slotRepository.existsByName("1st")).thenReturn(true);

        assertThrows(ApiException.class, () -> service.create(request("1st", "11:00", "11:45")));
    }

    @Test
    void endBeforeStartIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.create(request("Bad", "11:00", "10:00")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void slotCanBeEditedWithoutClashingWithItself() {
        service.update(1L, request("1st", "09:00", "09:50"));
    }

    @Test
    void usedSlotCannotBeDeleted() {
        when(timetableRepository.existsByTimeSlotId(1L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.delete(1L));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(slotRepository, never()).delete(any());
    }
}