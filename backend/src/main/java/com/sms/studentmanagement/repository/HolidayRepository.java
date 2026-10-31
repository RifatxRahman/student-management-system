package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

    List<Holiday> findByHolidayDateBetweenOrderByHolidayDate(LocalDate fromDate, LocalDate toDate);
}