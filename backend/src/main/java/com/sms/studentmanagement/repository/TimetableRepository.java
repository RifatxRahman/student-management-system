package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.TimetableEntry;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {

    @Query("""
            select t from TimetableEntry t
            join fetch t.timeSlot slot
            join fetch t.subject
            join fetch t.teacher
            left join fetch t.room
            join fetch t.section sec
            join fetch sec.schoolClass
            join fetch t.session
            where t.session.id = :sessionId and t.section.id = :sectionId
            order by slot.startTime
            """)
    List<TimetableEntry> findForSection(
            @Param("sessionId") Long sessionId, @Param("sectionId") Long sectionId);

    @Query("""
            select t from TimetableEntry t
            join fetch t.timeSlot slot
            join fetch t.subject
            join fetch t.teacher
            left join fetch t.room
            join fetch t.section sec
            join fetch sec.schoolClass
            join fetch t.session
            where t.session.id = :sessionId and t.teacher.id = :teacherId
            order by slot.startTime
            """)
    List<TimetableEntry> findForTeacher(
            @Param("sessionId") Long sessionId, @Param("teacherId") Long teacherId);

    @Query("""
            select t from TimetableEntry t
            join fetch t.timeSlot
            join fetch t.subject
            join fetch t.teacher
            left join fetch t.room
            join fetch t.section sec
            join fetch sec.schoolClass
            join fetch t.session
            where t.id = :id
            """)
    Optional<TimetableEntry> findWithRefs(@Param("id") Long id);

    Optional<TimetableEntry> findBySessionIdAndSectionIdAndDayOfWeekAndTimeSlotId(
            Long sessionId, Long sectionId, DayOfWeek dayOfWeek, Long timeSlotId);

    Optional<TimetableEntry> findBySessionIdAndTeacherIdAndDayOfWeekAndTimeSlotId(
            Long sessionId, Long teacherId, DayOfWeek dayOfWeek, Long timeSlotId);

    Optional<TimetableEntry> findBySessionIdAndRoomIdAndDayOfWeekAndTimeSlotId(
            Long sessionId, Long roomId, DayOfWeek dayOfWeek, Long timeSlotId);

    boolean existsByTimeSlotId(Long timeSlotId);

    boolean existsByRoomId(Long roomId);
}