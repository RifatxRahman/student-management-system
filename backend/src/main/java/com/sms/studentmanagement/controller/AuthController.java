package com.sms.studentmanagement.controller;

import com.sms.studentmanagement.config.AppProperties;
import com.sms.studentmanagement.dto.ChangePasswordRequest;
import com.sms.studentmanagement.dto.LoginRequest;
import com.sms.studentmanagement.dto.LoginResponse;
import com.sms.studentmanagement.dto.UserResponse;
import com.sms.studentmanagement.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final AppProperties props;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.Tokens tokens = authService.login(request.username().trim(), request.password());
        setRefreshCookie(response, tokens.refreshToken(), Duration.ofDays(props.security().refreshTokenDays()));
        return toResponse(tokens);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.Tokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken(), Duration.ofDays(props.security().refreshTokenDays()));
        return toResponse(tokens);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        setRefreshCookie(response, "", Duration.ZERO);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(authService.getUser(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        authService.changePassword(
                Long.valueOf(jwt.getSubject()), request.currentPassword(), request.newPassword());
        setRefreshCookie(response, "", Duration.ZERO);
    }

    private LoginResponse toResponse(AuthService.Tokens tokens) {
        return new LoginResponse(
                tokens.accessToken(),
                props.security().accessTokenMinutes() * 60L,
                UserResponse.from(tokens.user()));
    }

    private void setRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.security().cookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}