package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.RbacDtos.PermissionDto;
import com.sms.studentmanagement.dto.RbacDtos.RolePermissionMatrixResponse;
import com.sms.studentmanagement.dto.RbacDtos.UpdateRolePermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UpdateUserPermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UserEffectivePermissionsResponse;
import com.sms.studentmanagement.dto.RbacDtos.UserPermissionOverrideItem;
import com.sms.studentmanagement.entity.Permission;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.RolePermission;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserPermission;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.repository.PermissionRepository;
import com.sms.studentmanagement.repository.RolePermissionRepository;
import com.sms.studentmanagement.repository.UserPermissionRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    PermissionRepository permissionRepository;

    @Mock
    RolePermissionRepository rolePermissionRepository;

    @Mock
    UserPermissionRepository userPermissionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuditService auditService;

    RbacService service;

    Permission p1;
    Permission p2;
    Permission p3;

    @BeforeEach
    void setUp() {
        service = new RbacService(
                permissionRepository,
                rolePermissionRepository,
                userPermissionRepository,
                userRepository,
                auditService
        );

        p1 = new Permission();
        p1.setId(1L);
        p1.setCode("STUDENTS_VIEW");
        p1.setName("শিক্ষার্থী তালিকা দেখা");
        p1.setModule("STUDENTS");

        p2 = new Permission();
        p2.setId(2L);
        p2.setCode("FEES_COLLECT");
        p2.setName("ফি আদায় ও ইনভয়েস");
        p2.setModule("FEES");

        p3 = new Permission();
        p3.setId(3L);
        p3.setCode("SYSTEM_CONFIG");
        p3.setName("সিস্টেম কনফিগারেশন");
        p3.setModule("ADMIN");
    }

    @Test
    void listAllPermissions_returnsMappedDtos() {
        List<Permission> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);
        when(permissionRepository.findAllByOrderByModuleAscCodeAsc()).thenReturn(list);

        List<PermissionDto> dtos = service.listAllPermissions();
        assertEquals(2, dtos.size());
        assertEquals("STUDENTS_VIEW", dtos.get(0).code());
        assertEquals("FEES_COLLECT", dtos.get(1).code());
    }

    @Test
    void getRoleMatrix_returnsAllRolesWithAssignedPermissions() {
        RolePermission rp1 = new RolePermission();
        rp1.setId(10L);
        rp1.setRole(Role.TEACHER);
        rp1.setPermission(p1);

        List<RolePermission> teacherPerms = new ArrayList<>();
        teacherPerms.add(rp1);

        when(rolePermissionRepository.findByRoleWithPermission(Role.TEACHER)).thenReturn(teacherPerms);
        when(rolePermissionRepository.findByRoleWithPermission(Role.ADMIN)).thenReturn(new ArrayList<>());
        when(rolePermissionRepository.findByRoleWithPermission(Role.ACCOUNTANT)).thenReturn(new ArrayList<>());
        when(rolePermissionRepository.findByRoleWithPermission(Role.STAFF)).thenReturn(new ArrayList<>());

        List<RolePermissionMatrixResponse> matrix = service.getRoleMatrix();
        assertEquals(Role.values().length, matrix.size());

        RolePermissionMatrixResponse teacherMatrix = null;
        for (int i = 0; i < matrix.size(); i++) {
            if (matrix.get(i).role() == Role.TEACHER) {
                teacherMatrix = matrix.get(i);
                break;
            }
        }
        assertNotNull(teacherMatrix);
        assertEquals(1, teacherMatrix.permissions().size());
        assertEquals(1L, teacherMatrix.permissions().get(0).id());
    }

    @Test
    void updateRolePermissions_updatesMappingsAndAudits() {
        List<Long> assignedIds = new ArrayList<>();
        assignedIds.add(1L);
        assignedIds.add(2L);

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(Role.ACCOUNTANT, assignedIds);

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(p2));

        List<RolePermissionMatrixResponse> result = service.updateRolePermissions(request);

        verify(rolePermissionRepository).deleteByRole(Role.ACCOUNTANT);
        verify(rolePermissionRepository, times(2)).save(any(RolePermission.class));
        verify(auditService).log(eq("ROLE_PERMISSIONS_UPDATED"), eq("ROLE"), any(), any());
        assertNotNull(result);
    }

    @Test
    void getUserEffectivePermissions_resolvesRoleDefaultsAndCustomOverrides() {
        User user = new User();
        user.setId(5L);
        user.setUsername("teacher1");
        user.setFullName("আব্দুল করিম");
        user.setRole(Role.TEACHER);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        List<Permission> allPerms = new ArrayList<>();
        allPerms.add(p1);
        allPerms.add(p2);
        when(permissionRepository.findAllByOrderByModuleAscCodeAsc()).thenReturn(allPerms);

        // Role default: p1 granted to TEACHER
        RolePermission rp1 = new RolePermission();
        rp1.setId(101L);
        rp1.setRole(Role.TEACHER);
        rp1.setPermission(p1);
        List<RolePermission> rolePerms = new ArrayList<>();
        rolePerms.add(rp1);
        when(rolePermissionRepository.findByRoleWithPermission(Role.TEACHER)).thenReturn(rolePerms);

        // User override: p2 explicitly granted to user
        UserPermission up2 = new UserPermission();
        up2.setId(201L);
        up2.setUser(user);
        up2.setPermission(p2);
        up2.setGranted(true);
        List<UserPermission> userOverrides = new ArrayList<>();
        userOverrides.add(up2);
        when(userPermissionRepository.findByUserIdWithPermission(5L)).thenReturn(userOverrides);

        UserEffectivePermissionsResponse response = service.getUserEffectivePermissions(5L);
        assertNotNull(response);
        assertEquals(5L, response.userId());
        assertEquals("teacher1", response.username());
        assertEquals(3, response.effectiveAuthorities().size());
        assertTrue(response.effectiveAuthorities().contains("ROLE_TEACHER"));
        assertTrue(response.effectiveAuthorities().contains("STUDENTS_VIEW"));
        assertTrue(response.effectiveAuthorities().contains("FEES_COLLECT"));
    }

    @Test
    void getUserAuthorities_includesRolePrefixAndEffectiveCodes() {
        User user = new User();
        user.setId(8L);
        user.setUsername("accountant1");
        user.setRole(Role.ACCOUNTANT);

        RolePermission rp2 = new RolePermission();
        rp2.setId(102L);
        rp2.setRole(Role.ACCOUNTANT);
        rp2.setPermission(p2);
        List<RolePermission> rolePerms = new ArrayList<>();
        rolePerms.add(rp2);
        when(rolePermissionRepository.findByRoleWithPermission(Role.ACCOUNTANT)).thenReturn(rolePerms);

        List<UserPermission> overrides = new ArrayList<>();
        when(userPermissionRepository.findByUserIdWithPermission(8L)).thenReturn(overrides);

        List<Permission> allPerms = new ArrayList<>();
        allPerms.add(p1);
        allPerms.add(p2);
        when(permissionRepository.findAll()).thenReturn(allPerms);

        List<String> authorities = service.getUserAuthorities(user);
        assertTrue(authorities.contains("ROLE_ACCOUNTANT"));
        assertTrue(authorities.contains("FEES_COLLECT"));
        assertFalse(authorities.contains("STUDENTS_VIEW"));
    }

    @Test
    void updateUserPermissions_savesOverridesSuccessfully() {
        User user = new User();
        user.setId(12L);
        user.setUsername("staff1");
        user.setFullName("রহিম উদ্দিন");
        user.setRole(Role.STAFF);

        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p1));

        List<UserPermissionOverrideItem> overrides = new ArrayList<>();
        overrides.add(new UserPermissionOverrideItem(1L, true));
        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest(overrides);

        List<Permission> allPerms = new ArrayList<>();
        allPerms.add(p1);
        when(permissionRepository.findAllByOrderByModuleAscCodeAsc()).thenReturn(allPerms);
        when(rolePermissionRepository.findByRoleWithPermission(Role.STAFF)).thenReturn(new ArrayList<>());
        when(userPermissionRepository.findByUserIdWithPermission(12L)).thenReturn(new ArrayList<>());

        UserEffectivePermissionsResponse response = service.updateUserPermissions(12L, request);

        verify(userPermissionRepository).deleteByUserId(12L);
        verify(userPermissionRepository).save(any(UserPermission.class));
        verify(auditService).log(eq("USER_PERMISSIONS_OVERRIDDEN"), eq("USER"), eq(12L), any());
        assertNotNull(response);
    }
}
