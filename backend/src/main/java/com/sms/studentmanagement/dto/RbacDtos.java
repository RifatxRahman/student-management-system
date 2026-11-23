package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Permission;
import com.sms.studentmanagement.entity.Role;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class RbacDtos {

    private RbacDtos() {}

    public record PermissionDto(
            Long id,
            String name,
            String code,
            String module,
            String description
    ) {
        public static PermissionDto from(Permission p) {
            return new PermissionDto(
                    p.getId(),
                    p.getName(),
                    p.getCode(),
                    p.getModule(),
                    p.getDescription()
            );
        }
    }

    public record RolePermissionMatrixResponse(
            Role role,
            String roleBangla,
            List<PermissionDto> permissions
    ) {}

    public record UpdateRolePermissionsRequest(
            @NotNull(message = "ভূমিকা নির্বাচন আবশ্যক") Role role,
            @NotNull(message = "অনুমোদন তালিকা আবশ্যক") List<Long> permissionIds
    ) {}

    public record UserPermissionDetailDto(
            Long permissionId,
            String permissionName,
            String permissionCode,
            String module,
            boolean granted,
            boolean isRoleDefault
    ) {}

    public record UserEffectivePermissionsResponse(
            Long userId,
            String username,
            String fullName,
            Role role,
            String roleBangla,
            List<String> effectiveAuthorities,
            List<UserPermissionDetailDto> allPermissions
    ) {}

    public record UserPermissionOverrideItem(
            @NotNull Long permissionId,
            boolean granted
    ) {}

    public record UpdateUserPermissionsRequest(
            @NotNull List<UserPermissionOverrideItem> overrides
    ) {}
}
