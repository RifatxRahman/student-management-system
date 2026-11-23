package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.LeaveApplication;
import com.sms.studentmanagement.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    @Query("select l from LeaveApplication l join fetch l.teacher where l.teacher.id = :teacherId order by l.appliedAt desc")
    List<LeaveApplication> findByTeacherIdWithTeacher(@Param("teacherId") Long teacherId);

    @Query("select l from LeaveApplication l join fetch l.teacher order by l.appliedAt desc")
    List<LeaveApplication> findAllWithTeacher();

    @Query("select l from LeaveApplication l join fetch l.teacher where l.status = :status order by l.appliedAt desc")
    List<LeaveApplication> findByStatusWithTeacher(@Param("status") LeaveStatus status);

    @Query("select count(l) > 0 from LeaveApplication l where l.teacher.id = :teacherId " +
           "and l.status in ('PENDING', 'APPROVED') " +
           "and ((l.startDate between :startDate and :endDate) or (l.endDate between :startDate and :endDate) " +
           "or (:startDate between l.startDate and l.endDate))")
    boolean hasOverlappingLeave(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select l from LeaveApplication l where l.teacher.id = :teacherId " +
           "and l.status = 'APPROVED' and :date between l.startDate and l.endDate")
    Optional<LeaveApplication> findApprovedLeaveOnDate(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date);

    @Query("select l from LeaveApplication l join fetch l.teacher where l.status = 'APPROVED' and :date between l.startDate and l.endDate")
    List<LeaveApplication> findAllApprovedLeavesOnDate(@Param("date") LocalDate date);
}
