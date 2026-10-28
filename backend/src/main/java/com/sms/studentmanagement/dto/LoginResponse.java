package com.sms.studentmanagement.dto;

public record LoginResponse(String accessToken, long expiresInSeconds, UserResponse user) {
}