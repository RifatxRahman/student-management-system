package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.config.AppProperties;
import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.RefreshTokenRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    JwtService jwtService;

    PasswordEncoder encoder = new BCryptPasswordEncoder();
    AuthService service;
    User user;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Security("x".repeat(40), 15, 7, false, 3, 15),
                new AppProperties.Bootstrap("", ""));
        service = new AuthService(userRepository, refreshTokenRepository, encoder, jwtService, props);
        service.init();

        user = new User();
        user.setUsername("teacher1");
        user.setPasswordHash(encoder.encode("Correct123"));
        user.setRole(Role.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void locksAccountAfterTooManyWrongPasswords() {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));

        for (int i = 0; i < 3; i++) {
            assertThrows(ApiException.class, () -> service.login("teacher1", "wrong"));
        }
        ApiException locked = assertThrows(ApiException.class,
                () -> service.login("teacher1", "Correct123"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());
    }

    @Test
    void wrongUsernameAndWrongPasswordGiveSameMessage() {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        ApiException wrongPassword = assertThrows(ApiException.class,
                () -> service.login("teacher1", "wrong"));
        ApiException wrongUser = assertThrows(ApiException.class,
                () -> service.login("nobody", "wrong"));

        assertEquals(wrongPassword.getMessage(), wrongUser.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, wrongUser.getStatus());
    }

    @Test
    void deactivatedUserCannotLoginEvenWithCorrectPassword() {
        user.setStatus(UserStatus.DEACTIVATED);
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login("teacher1", "Correct123"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}