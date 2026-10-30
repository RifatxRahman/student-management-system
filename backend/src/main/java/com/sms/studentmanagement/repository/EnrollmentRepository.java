package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Enrollment;
import com.sms.studentmanagement.entity.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("""
            select e from Enrollment e
            join fetch e.session
            join fetch e.schoolClass
            join fetch e.section
            where e.student.id = :studentId
            order by e.startDate desc, e.id desc
            """)
    List<Enrollment> findAllByStudent(@Param("studentId") Long studentId);

    @Query("""
            select e from Enrollment e
            join fetch e.schoolClass
            join fetch e.section
            where e.student.id in :studentIds
              and e.session.id = :sessionId
              and e.status = :status
            """)
    List<Enrollment> findForStudents(
            @Param("studentIds") Collection<Long> studentIds,
            @Param("sessionId") Long sessionId,
            @Param("status") EnrollmentStatus status);

    @Query("select e from Enrollment e where e.student.id = :studentId and e.status = :status")
    List<Enrollment> findByStudentAndStatus(
            @Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);

    boolean existsByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    boolean existsByStudentIdAndSessionIdAndStatus(Long studentId, Long sessionId, EnrollmentStatus status);

    @Query("""
            select coalesce(max(e.rollNumber), 0) from Enrollment e
            where e.session.id = :sessionId and e.section.id = :sectionId and e.status = :status
            """)
    int maxRoll(
            @Param("sessionId") Long sessionId,
            @Param("sectionId") Long sectionId,
            @Param("status") EnrollmentStatus status);
}