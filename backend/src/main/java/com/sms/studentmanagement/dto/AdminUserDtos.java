package com.sms.studentmanagement.dto;

import com.sms.studentmanagement.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                    message = "ইউজারনেমে শুধু ইংরেজি অক্ষর, সংখ্যা, ডট, আন্ডারস্কোর ও হাইফেন চলবে")
            String username,
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Role role) {
    }

    public record CreatedUserResponse(UserResponse user, String temporaryPassword) {
    }

    public record TemporaryPasswordResponse(String temporaryPassword) {
    }
}