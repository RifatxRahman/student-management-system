package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Exam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("""
            select e from Exam e
            join fetch e.session ses
            join fetch e.schoolClass cls
            where (:sessionId is null or ses.id = :sessionId)
              and (:classId is null or cls.id = :classId)
            order by ses.startDate desc, cls.displayOrder, e.id desc
            """)
    List<Exam> search(@Param("sessionId") Long sessionId, @Param("classId") Long classId);

    @Query("""
            select e from Exam e
            join fetch e.session
            join fetch e.schoolClass
            where e.id = :id
            """)
    Optional<Exam> findWithRefs(@Param("id") Long id);

    boolean existsBySessionIdAndSchoolClassIdAndName(Long sessionId, Long classId, String name);

    boolean existsBySessionIdAndSchoolClassIdAndNameAndIdNot(Long sessionId, Long classId, String name, Long id);
}