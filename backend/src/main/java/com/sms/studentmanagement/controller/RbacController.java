package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.dto.RbacDtos.PermissionDto;
import com.sms.studentmanagement.dto.RbacDtos.RolePermissionMatrixResponse;
import com.sms.studentmanagement.dto.RbacDtos.UpdateRolePermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UpdateUserPermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UserEffectivePermissionsResponse;
import com.sms.studentmanagement.service.RbacService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> listAllPermissions() {
        return ResponseEntity.ok(rbacService.listAllPermissions());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolePermissionMatrixResponse>> getRoleMatrix() {
        return ResponseEntity.ok(rbacService.getRoleMatrix());
    }

    @PutMapping("/roles")
    public ResponseEntity<List<RolePermissionMatrixResponse>> updateRolePermissions(
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(rbacService.updateRolePermissions(request));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserEffectivePermissionsResponse> getUserPermissions(@PathVariable Long userId) {
        return ResponseEntity.ok(rbacService.getUserEffectivePermissions(userId));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserEffectivePermissionsResponse> updateUserPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserPermissionsRequest request) {
        return ResponseEntity.ok(rbacService.updateUserPermissions(userId, request));
    }
}
