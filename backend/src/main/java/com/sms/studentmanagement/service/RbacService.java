package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.RbacDtos.PermissionDto;
import com.sms.studentmanagement.dto.RbacDtos.RolePermissionMatrixResponse;
import com.sms.studentmanagement.dto.RbacDtos.UpdateRolePermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UpdateUserPermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UserEffectivePermissionsResponse;
import com.sms.studentmanagement.dto.RbacDtos.UserPermissionDetailDto;
import com.sms.studentmanagement.dto.RbacDtos.UserPermissionOverrideItem;
import com.sms.studentmanagement.entity.Permission;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.RolePermission;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserPermission;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.PermissionRepository;
import com.sms.studentmanagement.repository.RolePermissionRepository;
import com.sms.studentmanagement.repository.UserPermissionRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ==================== 1. Permissions Listing ====================

    @Transactional(readOnly = true)
    public List<PermissionDto> listAllPermissions() {
        List<Permission> list = permissionRepository.findAllByOrderByModuleAscCodeAsc();
        List<PermissionDto> dtos = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            dtos.add(PermissionDto.from(list.get(i)));
        }
        return dtos;
    }

    // ==================== 2. Role Permissions Matrix ====================

    @Transactional(readOnly = true)
    public List<RolePermissionMatrixResponse> getRoleMatrix() {
        Role[] roles = Role.values();
        List<RolePermissionMatrixResponse> matrix = new ArrayList<>();

        for (int i = 0; i < roles.length; i++) {
            Role role = roles[i];
            List<RolePermission> rolePerms = rolePermissionRepository.findByRoleWithPermission(role);
            List<PermissionDto> permDtos = new ArrayList<>();
            for (int j = 0; j < rolePerms.size(); j++) {
                permDtos.add(PermissionDto.from(rolePerms.get(j).getPermission()));
            }
            matrix.add(new RolePermissionMatrixResponse(role, role.getBanglaLabel(), permDtos));
        }
        return matrix;
    }

    @Transactional
    public List<RolePermissionMatrixResponse> updateRolePermissions(UpdateRolePermissionsRequest request) {
        Role role = request.role();
        rolePermissionRepository.deleteByRole(role);

        List<Long> permIds = request.permissionIds();
        for (int i = 0; i < permIds.size(); i++) {
            Long pId = permIds.get(i);
            Permission p = permissionRepository.findById(pId)
                    .orElseThrow(() -> new NotFoundException("অনুমোদন পাওয়া যায়নি: " + pId));

            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setPermission(p);
            rolePermissionRepository.save(rp);
        }

        auditService.log("ROLE_PERMISSIONS_UPDATED", "ROLE", (long) role.ordinal(),
                role.name() + " রোলে " + permIds.size() + " টি পারমিশন ধার্য করা হয়েছে।");

        return getRoleMatrix();
    }

    // ==================== 3. User Effective Permissions ====================

    @Transactional(readOnly = true)
    public UserEffectivePermissionsResponse getUserEffectivePermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("ব্যবহারকারী পাওয়া যায়নি: " + userId));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByModuleAscCodeAsc();
        List<RolePermission> rolePerms = rolePermissionRepository.findByRoleWithPermission(user.getRole());
        Set<Long> rolePermIds = new HashSet<>();
        for (int i = 0; i < rolePerms.size(); i++) {
            rolePermIds.add(rolePerms.get(i).getPermission().getId());
        }

        List<UserPermission> userOverrides = userPermissionRepository.findByUserIdWithPermission(userId);
        Map<Long, Boolean> overrideMap = new HashMap<>();
        for (int i = 0; i < userOverrides.size(); i++) {
            UserPermission up = userOverrides.get(i);
            overrideMap.put(up.getPermission().getId(), up.isGranted());
        }

        List<String> effectiveAuthorities = new ArrayList<>();
        effectiveAuthorities.add("ROLE_" + user.getRole().name());

        List<UserPermissionDetailDto> detailedList = new ArrayList<>();
        for (int i = 0; i < allPermissions.size(); i++) {
            Permission p = allPermissions.get(i);
            boolean isRoleDefault = rolePermIds.contains(p.getId());
            boolean effectiveGranted;

            if (overrideMap.containsKey(p.getId())) {
                effectiveGranted = overrideMap.get(p.getId());
            } else {
                effectiveGranted = isRoleDefault;
            }

            if (effectiveGranted) {
                effectiveAuthorities.add(p.getCode());
            }

            detailedList.add(new UserPermissionDetailDto(
                    p.getId(),
                    p.getName(),
                    p.getCode(),
                    p.getModule(),
                    effectiveGranted,
                    isRoleDefault
            ));
        }

        return new UserEffectivePermissionsResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getRole().getBanglaLabel(),
                effectiveAuthorities,
                detailedList
        );
    }

    @Transactional(readOnly = true)
    public List<String> getUserAuthorities(User user) {
        List<RolePermission> rolePerms = rolePermissionRepository.findByRoleWithPermission(user.getRole());
        Set<Long> rolePermIds = new HashSet<>();
        for (int i = 0; i < rolePerms.size(); i++) {
            rolePermIds.add(rolePerms.get(i).getPermission().getId());
        }

        List<UserPermission> userOverrides = userPermissionRepository.findByUserIdWithPermission(user.getId());
        Map<Long, Boolean> overrideMap = new HashMap<>();
        for (int i = 0; i < userOverrides.size(); i++) {
            UserPermission up = userOverrides.get(i);
            overrideMap.put(up.getPermission().getId(), up.isGranted());
        }

        List<Permission> allPermissions = permissionRepository.findAll();
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getRole().name());

        for (int i = 0; i < allPermissions.size(); i++) {
            Permission p = allPermissions.get(i);
            boolean isRoleDefault = rolePermIds.contains(p.getId());
            boolean granted = overrideMap.containsKey(p.getId()) ? overrideMap.get(p.getId()) : isRoleDefault;
            if (granted) {
                authorities.add(p.getCode());
            }
        }
        return authorities;
    }

    @Transactional
    public UserEffectivePermissionsResponse updateUserPermissions(Long userId, UpdateUserPermissionsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("ব্যবহারকারী পাওয়া যায়নি: " + userId));

        userPermissionRepository.deleteByUserId(userId);

        List<UserPermissionOverrideItem> overrides = request.overrides();
        for (int i = 0; i < overrides.size(); i++) {
            UserPermissionOverrideItem item = overrides.get(i);
            Permission p = permissionRepository.findById(item.permissionId())
                    .orElseThrow(() -> new NotFoundException("অনুমোদন পাওয়া যায়নি: " + item.permissionId()));

            UserPermission up = new UserPermission();
            up.setUser(user);
            up.setPermission(p);
            up.setGranted(item.granted());
            userPermissionRepository.save(up);
        }

        auditService.log("USER_PERMISSIONS_OVERRIDDEN", "USER", userId,
                user.getUsername() + " ইউজারের জন্য " + overrides.size() + " টি কাস্টম পারমিশন ওভাররাইড সংরক্ষিত হয়েছে।");

        return getUserEffectivePermissions(userId);
    }
}
