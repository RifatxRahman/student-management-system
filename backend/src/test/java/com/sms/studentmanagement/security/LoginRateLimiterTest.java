package com.sms.studentmanagement.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LoginRateLimiterTest {

    LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter();
    }

    @Test
    void allowsRequestsWithinLimit() {
        String testIp = "192.168.1.10";
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(testIp));
        }
    }

    @Test
    void blocksRequestsExceedingLimit() {
        String testIp = "192.168.1.50";
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(testIp);
        }

        ApiException ex = assertThrows(ApiException.class, () -> rateLimiter.checkRateLimit(testIp));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void resetClearsAttempts() {
        String testIp = "192.168.1.99";
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(testIp);
        }

        rateLimiter.reset(testIp);
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(testIp));
    }
}
