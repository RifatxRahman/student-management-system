package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.dto.UserResponse;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.RefreshTokenRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, refreshTokenRepository, new BCryptPasswordEncoder());
    }

    private User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void adminCannotDeactivateThemselves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN)));

        assertThrows(ApiException.class, () -> service.deactivate(1L, 1L));
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN)));
        when(userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        assertThrows(ApiException.class, () -> service.deactivate(1L, 2L));
    }

    @Test
    void deactivatingTeacherRevokesTheirLogins() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, Role.TEACHER)));

        UserResponse response = service.deactivate(3L, 1L);

        assertEquals(UserStatus.DEACTIVATED, response.status());
        verify(refreshTokenRepository).revokeAllActive(eq(3L), any(Instant.class));
    }
}