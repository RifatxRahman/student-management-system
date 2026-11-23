package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.AcademicEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicEventRepository extends JpaRepository<AcademicEvent, Long> {

    @Query("select e from AcademicEvent e join fetch e.session where e.session.id = :sessionId order by e.startDate asc")
    List<AcademicEvent> findBySessionIdWithSession(@Param("sessionId") Long sessionId);

    @Query("select e from AcademicEvent e join fetch e.session where (e.startDate <= :to and e.endDate >= :from) order by e.startDate asc")
    List<AcademicEvent> findInRangeWithSession(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from AcademicEvent e join fetch e.session where e.session.id = :sessionId and (e.startDate <= :to and e.endDate >= :from) order by e.startDate asc")
    List<AcademicEvent> findBySessionIdAndRangeWithSession(
            @Param("sessionId") Long sessionId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
