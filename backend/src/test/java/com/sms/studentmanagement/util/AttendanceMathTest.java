package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AttendanceMathTest {

    @Test
    void noCountedDaysGivesNull() {
        assertNull(AttendanceMath.percentage(0, 0, 0));
    }

    @Test
    void allPresentIsHundred() {
        assertEquals(new BigDecimal("100.00"), AttendanceMath.percentage(5, 0, 0));
    }

    @Test
    void allAbsentIsZero() {
        assertEquals(new BigDecimal("0.00"), AttendanceMath.percentage(0, 0, 4));
    }

    @Test
    void lateCountsAsPresent() {
        // ১ উপস্থিত + ১ দেরি + ২ অনুপস্থিত = ২/৪
        assertEquals(new BigDecimal("50.00"), AttendanceMath.percentage(1, 1, 2));
    }

    @Test
    void roundsToTwoDecimals() {
        assertEquals(new BigDecimal("66.67"), AttendanceMath.percentage(2, 0, 1));
        assertEquals(new BigDecimal("33.33"), AttendanceMath.percentage(1, 0, 2));
        assertEquals(new BigDecimal("12.50"), AttendanceMath.percentage(1, 0, 7));
    }
}