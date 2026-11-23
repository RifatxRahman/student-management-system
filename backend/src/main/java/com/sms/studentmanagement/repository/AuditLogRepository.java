package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.AuditLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
            order by a.createdAt desc, a.id desc
            """)
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);

    @Query("""
            select a from AuditLog a
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:action is null or a.action = :action)
              and (:status is null or a.status = :status)
              and (:actorUserId is null or a.actorUserId = :actorUserId)
              and (:startDate is null or a.createdAt >= :startDate)
              and (:endDate is null or a.createdAt <= :endDate)
            order by a.createdAt desc, a.id desc
            """)
    Page<AuditLog> searchAdvanced(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("action") String action,
            @Param("status") String status,
            @Param("actorUserId") Long actorUserId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    List<AuditLog> findTop5ByOrderByCreatedAtDesc();
}
