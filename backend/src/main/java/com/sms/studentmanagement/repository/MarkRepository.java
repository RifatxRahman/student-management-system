package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Mark;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    @Query("""
            select m from Mark m
            join fetch m.enrollment e
            where m.examSubject.id = :examSubjectId
              and e.section.id = :sectionId
            """)
    List<Mark> findForSectionAndExamSubject(
            @Param("examSubjectId") Long examSubjectId, @Param("sectionId") Long sectionId);

    @Query("select m from Mark m where m.examSubject.exam.id = :examId")
    List<Mark> findAllForExam(@Param("examId") Long examId);

    boolean existsByExamSubjectExamId(Long examId);

    boolean existsByExamSubjectId(Long examSubjectId);

    @Query("select max(m.marksObtained) from Mark m where m.examSubject.id = :examSubjectId")
    BigDecimal maxMarks(@Param("examSubjectId") Long examSubjectId);
}