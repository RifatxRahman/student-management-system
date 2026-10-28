package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Strong123", "admin"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("Ab1", "admin"));
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("OnlyLetters", "admin"));
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("12345678", "admin"));
    }

    @Test
    void rejectsPasswordSameAsUsername() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("teacher123", "TEACHER123"));
    }

    @Test
    void rejectsTooLongPassword() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("a1".repeat(40), "admin"));
    }
}