package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.StudentWaiver;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentWaiverRepository extends JpaRepository<StudentWaiver, Long> {

    @Query("SELECT sw FROM StudentWaiver sw JOIN FETCH sw.student JOIN FETCH sw.session JOIN FETCH sw.feeHead WHERE sw.student.id = :studentId AND sw.session.id = :sessionId AND sw.active = true")
    List<StudentWaiver> findActiveByStudentAndSession(@Param("studentId") Long studentId, @Param("sessionId") Long sessionId);

    @Query("SELECT sw FROM StudentWaiver sw JOIN FETCH sw.student JOIN FETCH sw.session JOIN FETCH sw.feeHead WHERE sw.student.id = :studentId ORDER BY sw.id DESC")
    List<StudentWaiver> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT sw FROM StudentWaiver sw JOIN FETCH sw.student JOIN FETCH sw.session JOIN FETCH sw.feeHead WHERE sw.session.id = :sessionId ORDER BY sw.id DESC")
    List<StudentWaiver> findBySessionId(@Param("sessionId") Long sessionId);

    Optional<StudentWaiver> findByStudentIdAndSessionIdAndFeeHeadId(Long studentId, Long sessionId, Long feeHeadId);

    boolean existsByStudentIdAndSessionIdAndFeeHeadId(Long studentId, Long sessionId, Long feeHeadId);
}
