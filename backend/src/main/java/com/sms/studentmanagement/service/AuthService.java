package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.AppProperties;
import com.sms.studentmanagement.entity.RefreshToken;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.repository.RefreshTokenRepository;
import com.sms.studentmanagement.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;

    private String dummyHash;

    public record Tokens(String accessToken, String refreshToken, User user) {
    }

    @PostConstruct
    void init() {
        // ভুল ইউজারনেমেও একই সময় লাগানোর জন্য একটা নকল হ্যাশ
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Tokens login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw invalidCredentials();
        }

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "অনেকবার ভুল চেষ্টা হয়েছে। কিছুক্ষণ পরে আবার চেষ্টা করুন।");
        }

        boolean passwordOk = passwordEncoder.matches(password, user.getPasswordHash());
        if (!passwordOk || user.getStatus() != UserStatus.ACTIVE) {
            registerFailure(user, now);
            throw invalidCredentials();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Tokens refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidSession();
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(AuthService::invalidSession);
        Instant now = Instant.now();

        if (token.getRevokedAt() != null) {
            // একসাথে দুবার ডাকা হলে (১০ সেকেন্ডের মধ্যে) শুধু বাতিল করুন
            if (token.getRevokedAt().isBefore(now.minusSeconds(10))) {
                // পুরনো বাতিল চাবি আবার এসেছে: চুরির সন্দেহ, সব চাবি বাতিল
                refreshTokenRepository.revokeAllActive(token.getUser().getId(), now);
            }
            throw invalidSession();
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw invalidSession();
        }
        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidSession();
        }

        token.setRevokedAt(now);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "বর্তমান পাসওয়ার্ড ভুল");
        }
        PasswordPolicy.validate(newPassword, user.getUsername());
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "নতুন পাসওয়ার্ড আগেরটার মতো হতে পারবে না");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        refreshTokenRepository.revokeAllActive(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(AuthService::invalidSession);
    }

    private void registerFailure(User user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= props.security().maxFailedAttempts()) {
            user.setLockedUntil(now.plus(props.security().lockMinutes(), ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
        } else {
            user.setFailedLoginAttempts(attempts);
        }
    }

    private Tokens issueTokens(User user) {
        String raw = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(Instant.now().plus(props.security().refreshTokenDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(token);
        return new Tokens(jwtService.createAccessToken(user), raw, user);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "ইউজারনেম বা পাসওয়ার্ড ভুল");
    }

    private static ApiException invalidSession() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "সেশন শেষ হয়েছে, আবার লগইন করুন");
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}