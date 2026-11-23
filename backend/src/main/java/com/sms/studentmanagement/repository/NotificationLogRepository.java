package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.NotificationLog;
import com.sms.studentmanagement.entity.NotificationStatus;
import com.sms.studentmanagement.entity.NotificationType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByStudentIdAndNotificationTypeAndEventDate(
            Long studentId, NotificationType notificationType, LocalDate eventDate);

    List<NotificationLog> findByStatusOrderByIdAsc(NotificationStatus status, Pageable pageable);

    @Query("""
            select n from NotificationLog n
            where n.status = :status and n.attempts < :maxAttempts
            order by n.id asc
            """)
    List<NotificationLog> findRetryableFailed(
            @Param("status") NotificationStatus status,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable);

    long deleteByStudentIdAndNotificationTypeAndEventDateAndStatusIn(
            Long studentId, NotificationType notificationType, LocalDate eventDate,
            Collection<NotificationStatus> statuses);

    @Query(value = """
            select n from NotificationLog n
            left join fetch n.student
            left join fetch n.guardian
            where (:status is null or n.status = :status)
            order by n.id desc
            """,
            countQuery = """
            select count(n) from NotificationLog n
            where (:status is null or n.status = :status)
            """)
    Page<NotificationLog> search(@Param("status") NotificationStatus status, Pageable pageable);
}