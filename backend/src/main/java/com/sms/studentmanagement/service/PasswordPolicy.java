package com.sms.studentmanagement.service;

import com.sms.studentmanagement.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password, String username) {
        if (password == null || password.length() < 8) {
            throw bad("পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw bad("পাসওয়ার্ড খুব বড় (সর্বোচ্চ ৭২ বাইট)");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw bad("পাসওয়ার্ডে অন্তত একটি অক্ষর ও একটি সংখ্যা থাকতে হবে");
        }
        if (username != null && password.equalsIgnoreCase(username)) {
            throw bad("পাসওয়ার্ড ইউজারনেমের মতো হতে পারবে না");
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}