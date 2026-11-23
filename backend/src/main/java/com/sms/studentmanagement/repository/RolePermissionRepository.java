package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.permission WHERE rp.role = :role ORDER BY rp.permission.module ASC, rp.permission.code ASC")
    List<RolePermission> findByRoleWithPermission(@Param("role") Role role);

    @Query("SELECT rp.permission.code FROM RolePermission rp WHERE rp.role = :role")
    List<String> findPermissionCodesByRole(@Param("role") Role role);

    void deleteByRole(Role role);
}
