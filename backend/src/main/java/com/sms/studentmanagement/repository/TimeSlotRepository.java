package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.TimeSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findAllByOrderByStartTime();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}