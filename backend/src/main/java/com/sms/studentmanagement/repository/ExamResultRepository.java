package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.ExamResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @Query("""
            select r from ExamResult r
            join fetch r.enrollment e
            join fetch e.student
            join fetch e.section
            where r.exam.id = :examId
            """)
    List<ExamResult> findAllForExam(@Param("examId") Long examId);

    long deleteByExamId(Long examId);
}