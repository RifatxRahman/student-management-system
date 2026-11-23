package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByModuleAscCodeAsc();

    List<Permission> findByModuleOrderByCodeAsc(String module);
}
