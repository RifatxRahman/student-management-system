package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    @Test
    void generatedPasswordIsLongEnoughAndPassesPolicy() {
        for (int i = 0; i < 50; i++) {
            String password = TemporaryPasswordGenerator.generate();
            assertEquals(12, password.length());
            assertDoesNotThrow(() -> PasswordPolicy.validate(password, "teacher1"));
        }
    }

    @Test
    void generatedPasswordsAreDifferent() {
        assertNotEquals(TemporaryPasswordGenerator.generate(), TemporaryPasswordGenerator.generate());
    }
}