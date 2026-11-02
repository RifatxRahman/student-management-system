package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.ResultSubject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultSubjectRepository extends JpaRepository<ResultSubject, Long> {

    @Query("""
            select rs from ResultSubject rs
            join fetch rs.result
            join fetch rs.examSubject es
            join fetch es.subject
            where rs.result.exam.id = :examId
            """)
    List<ResultSubject> findAllForExam(@Param("examId") Long examId);

    long deleteByResultExamId(Long examId);
}