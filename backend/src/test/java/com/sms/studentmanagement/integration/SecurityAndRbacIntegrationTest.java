package com.sms.studentmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.RbacDtos.UpdateUserPermissionsRequest;
import com.sms.studentmanagement.dto.RbacDtos.UserEffectivePermissionsResponse;
import com.sms.studentmanagement.dto.RbacDtos.UserPermissionOverrideItem;
import com.sms.studentmanagement.entity.Permission;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.RolePermission;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserPermission;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.PermissionRepository;
import com.sms.studentmanagement.repository.RolePermissionRepository;
import com.sms.studentmanagement.repository.UserPermissionRepository;
import com.sms.studentmanagement.repository.UserRepository;
import com.sms.studentmanagement.security.LoginRateLimiter;
import com.sms.studentmanagement.service.AuditService;
import com.sms.studentmanagement.service.RbacService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SecurityAndRbacIntegrationTest {

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

    RbacService rbacService;
    LoginRateLimiter rateLimiter;

    Permission pFees;
    Permission pMarks;
    Permission pStudents;

    User teacherUser;
    User accountantUser;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService(
                permissionRepository,
                rolePermissionRepository,
                userPermissionRepository,
                userRepository,
                auditService
        );
        rateLimiter = new LoginRateLimiter();

        pFees = new Permission();
        pFees.setId(101L);
        pFees.setCode("FEES_COLLECT");
        pFees.setName("ফি আদায় ও ইনভয়েস");
        pFees.setModule("FEES");

        pMarks = new Permission();
        pMarks.setId(102L);
        pMarks.setCode("MARKS_ENTRY");
        pMarks.setName("নম্বর এন্ট্রি");
        pMarks.setModule("MARKS");

        pStudents = new Permission();
        pStudents.setId(103L);
        pStudents.setCode("STUDENTS_VIEW");
        pStudents.setName("শিক্ষার্থী তালিকা");
        pStudents.setModule("STUDENTS");

        teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setUsername("teacher_karim");
        teacherUser.setFullName("আব্দুল করিম");
        teacherUser.setRole(Role.TEACHER);
        teacherUser.setStatus(UserStatus.ACTIVE);

        accountantUser = new User();
        accountantUser.setId(2L);
        accountantUser.setUsername("accountant_rahim");
        accountantUser.setFullName("রহিম সাহেব");
        accountantUser.setRole(Role.ACCOUNTANT);
        accountantUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void customOverride_grantPermissionToTeacher_takesPrecedenceOverRoleDefault() {
        // শিক্ষকের ডিফল্ট পারমিশন: শুধু MARKS_ENTRY
        RolePermission rp = new RolePermission();
        rp.setId(1L);
        rp.setRole(Role.TEACHER);
        rp.setPermission(pMarks);
        List<RolePermission> teacherRolePerms = new ArrayList<>();
        teacherRolePerms.add(rp);

        // ইউজারের কাস্টম ওভাররাইড: FEES_COLLECT প্রদান (granted = true)
        UserPermission up = new UserPermission();
        up.setId(11L);
        up.setUser(teacherUser);
        up.setPermission(pFees);
        up.setGranted(true);
        List<UserPermission> overrides = new ArrayList<>();
        overrides.add(up);

        List<Permission> allPerms = new ArrayList<>();
        allPerms.add(pFees);
        allPerms.add(pMarks);
        allPerms.add(pStudents);

        when(rolePermissionRepository.findByRoleWithPermission(Role.TEACHER)).thenReturn(teacherRolePerms);
        when(userPermissionRepository.findByUserIdWithPermission(1L)).thenReturn(overrides);
        when(permissionRepository.findAll()).thenReturn(allPerms);

        List<String> authorities = rbacService.getUserAuthorities(teacherUser);

        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_TEACHER"));
        assertTrue(authorities.contains("MARKS_ENTRY"), "ডিফল্ট পারমিশন বহাল থাকতে হবে");
        assertTrue(authorities.contains("FEES_COLLECT"), "কাস্টম ওভাররাইডের মাধ্যমে ফি আদায়ের বিশেষ অনুমতি প্রযোজ্য হতে হবে");
        assertFalse(authorities.contains("STUDENTS_VIEW"), "অননুমোদিত পারমিশন পাওয়া যাবে না");
    }

    @Test
    void customOverride_revokePermissionFromAccountant_stripsDefaultRolePermission() {
        // হিসাবরক্ষকের ডিফল্ট পারমিশন: FEES_COLLECT
        RolePermission rp = new RolePermission();
        rp.setId(2L);
        rp.setRole(Role.ACCOUNTANT);
        rp.setPermission(pFees);
        List<RolePermission> accountantRolePerms = new ArrayList<>();
        accountantRolePerms.add(rp);

        // ইউজারের কাস্টম ওভাররাইড: FEES_COLLECT বাতিল (granted = false)
        UserPermission up = new UserPermission();
        up.setId(12L);
        up.setUser(accountantUser);
        up.setPermission(pFees);
        up.setGranted(false);
        List<UserPermission> overrides = new ArrayList<>();
        overrides.add(up);

        List<Permission> allPerms = new ArrayList<>();
        allPerms.add(pFees);
        allPerms.add(pMarks);

        when(rolePermissionRepository.findByRoleWithPermission(Role.ACCOUNTANT)).thenReturn(accountantRolePerms);
        when(userPermissionRepository.findByUserIdWithPermission(2L)).thenReturn(overrides);
        when(permissionRepository.findAll()).thenReturn(allPerms);

        List<String> authorities = rbacService.getUserAuthorities(accountantUser);

        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_ACCOUNTANT"));
        assertFalse(authorities.contains("FEES_COLLECT"), "কাস্টম ওভাররাইডে বাতিল করায় ডিফল্ট পারমিশন স্ট্রিপ হতে হবে!");
    }

    @Test
    void rateLimiterUnderConcurrentStress_blocksRequestsExceedingThreshold() throws Exception {
        int threads = 15;
        String clientIp = "10.0.0.12";
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startSignal.await();
                    rateLimiter.checkRateLimit(clientIp);
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.TOO_MANY_REQUESTS) {
                        blockedCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        doneSignal.await();
        pool.shutdown();

        assertEquals(10, successCount.get(), "সর্বোচ্চ ১০টি রিকোয়েস্ট অনুমোদিত হতে হবে!");
        assertEquals(5, blockedCount.get(), "বাকি ৫টি অতিরিক্ত রিকোয়েস্ট ৪২৯ স্ট্যাটাস দিয়ে ব্লক হতে হবে!");
    }
}
