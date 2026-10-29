package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.AcademicSessionRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicSessionServiceTest {

    @Mock
    AcademicSessionRepository repository;

    @Test
    void rejectsEndDateBeforeStartDate() {
        AcademicSessionService service = new AcademicSessionService(repository);

        assertThrows(ApiException.class, () -> service.create(
                "2026", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsSameStartAndEndDate() {
        AcademicSessionService service = new AcademicSessionService(repository);

        assertThrows(ApiException.class, () -> service.create(
                "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));
    }
}