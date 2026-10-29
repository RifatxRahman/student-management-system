package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.AcademicSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {

    Optional<AcademicSession> findByActiveTrue();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AcademicSession s set s.active = false, s.updatedAt = :now where s.active = true")
    int deactivateAll(@Param("now") Instant now);
}