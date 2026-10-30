package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.StudentGuardian;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    @Query("""
            select sg from StudentGuardian sg join fetch sg.guardian g
            where sg.student.id = :studentId
            order by sg.primaryContact desc, g.fullName
            """)
    List<StudentGuardian> findAllWithGuardian(@Param("studentId") Long studentId);

    @Query("""
            select sg from StudentGuardian sg join fetch sg.student s
            where sg.guardian.id = :guardianId
            order by s.fullName
            """)
    List<StudentGuardian> findAllForGuardian(@Param("guardianId") Long guardianId);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(Long studentId, Long guardianId);

    long countByStudentId(Long studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update StudentGuardian sg set sg.primaryContact = false, sg.updatedAt = :now
            where sg.student.id = :studentId and sg.primaryContact = true
            """)
    int clearPrimary(@Param("studentId") Long studentId, @Param("now") Instant now);
}