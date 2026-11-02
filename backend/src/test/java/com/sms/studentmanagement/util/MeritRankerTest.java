package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sms.studentmanagement.util.MeritRanker.Entry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MeritRankerTest {

    private static Entry entry(long id, boolean passed, String gpa, String total) {
        return new Entry(id, passed, new BigDecimal(gpa), new BigDecimal(total));
    }

    @Test
    void higherGpaComesFirst() {
        Map<Long, Integer> positions = MeritRanker.rank(List.of(
                entry(1, true, "4.00", "400"), entry(2, true, "5.00", "300"), entry(3, true, "4.50", "350")));

        assertEquals(1, positions.get(2L));
        assertEquals(2, positions.get(3L));
        assertEquals(3, positions.get(1L));
    }

    @Test
    void sameGpaIsDecidedByTotalMarks() {
        Map<Long, Integer> positions = MeritRanker.rank(List.of(
                entry(1, true, "5.00", "480"), entry(2, true, "5.00", "495")));

        assertEquals(1, positions.get(2L));
        assertEquals(2, positions.get(1L));
    }

    @Test
    void fullTieSharesPositionAndNextOneSkips() {
        Map<Long, Integer> positions = MeritRanker.rank(List.of(
                entry(1, true, "5.00", "490"), entry(2, true, "4.80", "450"),
                entry(3, true, "4.80", "450.00"), entry(4, true, "4.00", "400")));

        assertEquals(1, positions.get(1L));
        assertEquals(2, positions.get(2L));
        assertEquals(2, positions.get(3L));
        assertEquals(4, positions.get(4L));
    }

    @Test
    void failedStudentsAreNeverRanked() {
        Map<Long, Integer> positions = MeritRanker.rank(List.of(
                entry(1, true, "3.00", "300"), entry(2, false, "0.00", "250"), entry(3, true, "2.00", "200")));

        assertEquals(2, positions.size());
        assertFalse(positions.containsKey(2L));
        assertEquals(1, positions.get(1L));
        assertEquals(2, positions.get(3L));
    }

    @Test
    void emptyAndAllFailedGiveNoPositions() {
        assertTrue(MeritRanker.rank(List.of()).isEmpty());
        assertTrue(MeritRanker.rank(List.of(entry(1, false, "0.00", "100"))).isEmpty());
    }

    @Test
    void inputOrderDoesNotMatter() {
        Map<Long, Integer> a = MeritRanker.rank(List.of(
                entry(1, true, "4.00", "400"), entry(2, true, "5.00", "300")));
        Map<Long, Integer> b = MeritRanker.rank(List.of(
                entry(2, true, "5.00", "300"), entry(1, true, "4.00", "400")));

        assertEquals(a.get(1L), b.get(1L));
        assertEquals(a.get(2L), b.get(2L));
    }
}