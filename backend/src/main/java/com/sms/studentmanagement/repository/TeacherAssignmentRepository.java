package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.TeacherAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    boolean existsBySessionIdAndSectionIdAndSubjectId(Long sessionId, Long sectionId, Long subjectId);

    @Query("""
            select ta from TeacherAssignment ta
            join fetch ta.session ses
            join fetch ta.section sec
            join fetch sec.schoolClass cls
            join fetch ta.subject sub
            where ta.teacher.id = :teacherId
              and (:sessionId is null or ses.id = :sessionId)
            order by ses.startDate desc, cls.displayOrder, sec.name, sub.name
            """)
    List<TeacherAssignment> findAllForTeacher(
            @Param("teacherId") Long teacherId, @Param("sessionId") Long sessionId);
}