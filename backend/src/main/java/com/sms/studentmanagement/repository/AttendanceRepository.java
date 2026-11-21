package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.dto.AttendanceCountRow;
import com.sms.studentmanagement.entity.Attendance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("""
            select a from Attendance a
            join fetch a.enrollment e
            where e.section.id = :sectionId
              and a.attendanceDate = :date
            """)
    List<Attendance> findForSectionOnDate(
            @Param("sectionId") Long sectionId, @Param("date") LocalDate date);

    boolean existsByAttendanceDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("""
            select new com.sms.studentmanagement.dto.AttendanceCountRow(
                a.enrollment.id, a.status, count(a))
            from Attendance a
            where a.enrollment.section.id = :sectionId
              and a.attendanceDate between :fromDate and :toDate
            group by a.enrollment.id, a.status
            """)
    List<AttendanceCountRow> countByEnrollmentAndStatus(
            @Param("sectionId") Long sectionId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            select count(distinct a.attendanceDate) from Attendance a
            where a.enrollment.section.id = :sectionId
              and a.attendanceDate between :fromDate and :toDate
            """)
    long countMarkedDays(
            @Param("sectionId") Long sectionId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /** একজন ছাত্রের (enrollment) নির্দিষ্ট সময়ের দিনভিত্তিক হাজিরা, তারিখ অনুযায়ী। */
    @Query("""
            select a from Attendance a
            where a.enrollment.id = :enrollmentId
              and a.attendanceDate between :fromDate and :toDate
            order by a.attendanceDate
            """)
    List<Attendance> findByEnrollmentAndRange(
            @Param("enrollmentId") Long enrollmentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Modifying
    @Query("""
            update Attendance a set a.finalized = true
            where a.enrollment.section.id = :sectionId and a.attendanceDate = :date
            """)
    int finalizeBySectionAndDate(
            @Param("sectionId") Long sectionId,
            @Param("date") LocalDate date);

    @Query("""
            select new com.sms.studentmanagement.dto.AttendanceCountRow(
                0L, a.status, count(a))
            from Attendance a
            where a.attendanceDate = :date
            group by a.status
            """)
    List<AttendanceCountRow> countSchoolWideOnDate(@Param("date") LocalDate date);
}