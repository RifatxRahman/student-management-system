package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SmsCalculatorTest {

    @Test
    void detectsUnicodeInBanglaText() {
        assertTrue(SmsCalculator.isUnicode("সম্মানিত অভিভাবক"));
        assertFalse(SmsCalculator.isUnicode("Dear Guardian, Rahim was absent."));
    }

    @Test
    void calculatesGsmSingleAndMultiParts() {
        // GSM: 160 chars is 1 part
        String singleGsm = "A".repeat(160);
        assertEquals(1, SmsCalculator.calculateParts(singleGsm));

        // GSM: 161 chars is 2 parts (161 / 153 = 1.05 -> 2)
        String multiGsm = "A".repeat(161);
        assertEquals(2, SmsCalculator.calculateParts(multiGsm));
    }

    @Test
    void calculatesUnicodeSingleAndMultiParts() {
        // Unicode: 70 chars is 1 part
        String singleUnicode = "ক".repeat(70);
        assertEquals(1, SmsCalculator.calculateParts(singleUnicode));

        // Unicode: 71 chars is 2 parts (71 / 67 = 1.05 -> 2)
        String multiUnicode = "ক".repeat(71);
        assertEquals(2, SmsCalculator.calculateParts(multiUnicode));
    }

    @Test
    void calculatesCostWithBigDecimal() {
        BigDecimal rate = new BigDecimal("0.25");
        assertEquals(new BigDecimal("0.50"), SmsCalculator.calculateCost(2, rate));
        assertEquals(BigDecimal.ZERO, SmsCalculator.calculateCost(0, rate));
    }
}
