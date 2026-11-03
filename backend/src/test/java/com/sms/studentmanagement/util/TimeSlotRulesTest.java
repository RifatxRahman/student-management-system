package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sms.studentmanagement.exception.ApiException;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeSlotRulesTest {

    private static LocalTime t(String value) {
        return LocalTime.parse(value);
    }

    @Test
    void touchingSlotsDoNotOverlap() {
        assertFalse(TimeSlotRules.overlaps(t("09:00"), t("09:45"), t("09:45"), t("10:30")));
        assertFalse(TimeSlotRules.overlaps(t("09:45"), t("10:30"), t("09:00"), t("09:45")));
    }

    @Test
    void partialOverlapIsDetectedFromBothSides() {
        assertTrue(TimeSlotRules.overlaps(t("09:00"), t("09:45"), t("09:30"), t("10:15")));
        assertTrue(TimeSlotRules.overlaps(t("09:30"), t("10:15"), t("09:00"), t("09:45")));
    }

    @Test
    void containedSlotOverlaps() {
        assertTrue(TimeSlotRules.overlaps(t("09:00"), t("11:00"), t("09:30"), t("10:00")));
        assertTrue(TimeSlotRules.overlaps(t("09:30"), t("10:00"), t("09:00"), t("11:00")));
    }

    @Test
    void identicalSlotsOverlap() {
        assertTrue(TimeSlotRules.overlaps(t("09:00"), t("09:45"), t("09:00"), t("09:45")));
    }

    @Test
    void farApartSlotsDoNotOverlap() {
        assertFalse(TimeSlotRules.overlaps(t("09:00"), t("09:45"), t("13:00"), t("13:45")));
    }

    @Test
    void endMustBeAfterStart() {
        assertDoesNotThrow(() -> TimeSlotRules.validateRange(t("09:00"), t("09:01")));
        assertThrows(ApiException.class, () -> TimeSlotRules.validateRange(t("09:00"), t("09:00")));
        assertThrows(ApiException.class, () -> TimeSlotRules.validateRange(t("10:00"), t("09:00")));
    }
}