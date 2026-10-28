package com.sms.studentmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Bootstrap bootstrap) {

    public record Security(
            String jwtSecret,
            int accessTokenMinutes,
            int refreshTokenDays,
            boolean cookieSecure,
            int maxFailedAttempts,
            int lockMinutes) {
    }

    public record Bootstrap(String adminUsername, String adminPassword) {
    }
}