package com.sms.studentmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(max = 100) String newPassword) {
}