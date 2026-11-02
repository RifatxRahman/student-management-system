package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.ExamSubject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    @Query("""
            select es from ExamSubject es
            join fetch es.subject sub
            where es.exam.id = :examId
            order by sub.name
            """)
    List<ExamSubject> findAllWithSubject(@Param("examId") Long examId);

    @Query("""
            select es from ExamSubject es
            join fetch es.exam
            join fetch es.subject
            where es.id = :id
            """)
    Optional<ExamSubject> findWithRefs(@Param("id") Long id);

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    long deleteByExamId(Long examId);

    @Query("""
            select es from ExamSubject es
            join fetch es.subject
            where es.exam.id = :examId and es.subject.id = :subjectId
            """)
    Optional<ExamSubject> findByExamIdAndSubjectId(
            @Param("examId") Long examId, @Param("subjectId") Long subjectId);
}