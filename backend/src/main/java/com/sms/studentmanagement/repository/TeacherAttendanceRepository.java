package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.TeacherAttendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

    Optional<TeacherAttendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate attendanceDate);

    @Query("select ta from TeacherAttendance ta join fetch ta.teacher where ta.attendanceDate = :attendanceDate")
    List<TeacherAttendance> findByAttendanceDateWithTeacher(@Param("attendanceDate") LocalDate attendanceDate);

    List<TeacherAttendance> findByTeacherIdAndAttendanceDateBetween(Long teacherId, LocalDate startDate, LocalDate endDate);

    List<TeacherAttendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByTeacherIdAndAttendanceDate(Long teacherId, LocalDate attendanceDate);
}
