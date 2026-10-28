package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Role;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        UserStatus status,
        boolean mustChangePassword) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword());
    }
}