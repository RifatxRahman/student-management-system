package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sms.studentmanagement.exception.ApiException;
import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void normalizesDifferentFormats() {
        assertEquals("01712345678", PhoneNumbers.normalize("01712345678"));
        assertEquals("01712345678", PhoneNumbers.normalize("+8801712345678"));
        assertEquals("01712345678", PhoneNumbers.normalize("8801712345678"));
        assertEquals("01712345678", PhoneNumbers.normalize("017-1234-5678"));
        assertEquals("01712345678", PhoneNumbers.normalize("০১৭১২৩৪৫৬৭৮"));
    }

    @Test
    void blankBecomesNull() {
        assertNull(PhoneNumbers.normalize(null));
        assertNull(PhoneNumbers.normalize("  "));
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThrows(ApiException.class, () -> PhoneNumbers.normalize("12345"));
        assertThrows(ApiException.class, () -> PhoneNumbers.normalize("01212345678"));
        assertThrows(ApiException.class, () -> PhoneNumbers.normalize("0171234567a"));
        assertThrows(ApiException.class, () -> PhoneNumbers.normalize("017123456789"));
    }
}