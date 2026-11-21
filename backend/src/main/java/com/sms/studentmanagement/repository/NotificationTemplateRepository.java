package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.NotificationTemplate;
import com.sms.studentmanagement.entity.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<NotificationTemplate> findByActiveTrueOrderByNameAsc();

    List<NotificationTemplate> findAllByOrderByNameAsc();

    List<NotificationTemplate> findByCategoryAndActiveTrue(NotificationType category);
}
