package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.NotificationCampaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, Long> {

    @Query("SELECT c FROM NotificationCampaign c LEFT JOIN FETCH c.targetClass LEFT JOIN FETCH c.targetSection ORDER BY c.id DESC")
    List<NotificationCampaign> findAllWithDetails();

    @Query("SELECT c FROM NotificationCampaign c LEFT JOIN FETCH c.targetClass LEFT JOIN FETCH c.targetSection WHERE c.id = :id")
    Optional<NotificationCampaign> findByIdWithDetails(@Param("id") Long id);
}
