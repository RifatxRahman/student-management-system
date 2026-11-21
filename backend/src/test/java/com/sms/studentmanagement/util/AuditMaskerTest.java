package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuditMaskerTest {

    @Test
    void mask_protectsPasswordsAndTokens() {
        String logData = "{\"username\": \"john\", \"password\": \"SecretPass@123\", \"token\": \"Bearer abc.def.ghi\"}";
        String masked = AuditMasker.mask(logData);

        assertFalse(masked.contains("SecretPass@123"));
        assertTrue(masked.contains("***PROTECTED***"));
        assertTrue(masked.contains("Bearer [PROTECTED_TOKEN]"));
    }

    @Test
    void maskPhoneNumber_masksMiddleDigits() {
        String phone = "01712345678";
        String masked = AuditMasker.maskPhoneNumber(phone);

        assertEquals("017****5678", masked);
    }
}
