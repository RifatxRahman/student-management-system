package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradeCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private final List<GradeBand> scale = List.of(
            new GradeBand(bd("80"), "A+", bd("5.00")),
            new GradeBand(bd("70"), "A", bd("4.00")),
            new GradeBand(bd("60"), "A-", bd("3.50")),
            new GradeBand(bd("50"), "B", bd("3.00")),
            new GradeBand(bd("40"), "C", bd("2.00")),
            new GradeBand(bd("33"), "D", bd("1.00")),
            new GradeBand(bd("0"), "F", bd("0.00")));

    private String grade(String marks, String full) {
        return GradeCalculator.lookup(scale, bd(marks), bd(full)).grade();
    }

    @Test
    void topAndBottomValues() {
        assertEquals("A+", grade("100", "100"));
        assertEquals("F", grade("0", "100"));
    }

    @Test
    void exactlyOnTheBoundaryGoesToTheHigherGrade() {
        assertEquals("A+", grade("80", "100"));
        assertEquals("A", grade("70", "100"));
        assertEquals("A-", grade("60", "100"));
        assertEquals("B", grade("50", "100"));
        assertEquals("C", grade("40", "100"));
        assertEquals("D", grade("33", "100"));
    }

    @Test
    void justBelowTheBoundaryStaysInTheLowerGrade() {
        assertEquals("A", grade("79.99", "100"));
        assertEquals("A-", grade("69.99", "100"));
        assertEquals("B", grade("59.99", "100"));
        assertEquals("C", grade("49.99", "100"));
        assertEquals("D", grade("39.99", "100"));
        assertEquals("F", grade("32.99", "100"));
    }

    @Test
    void gradePointComesWithTheGrade() {
        assertEquals(bd("5.00"), GradeCalculator.lookup(scale, bd("92"), bd("100")).gradePoint());
        assertEquals(bd("3.50"), GradeCalculator.lookup(scale, bd("65"), bd("100")).gradePoint());
        assertEquals(bd("0.00"), GradeCalculator.lookup(scale, bd("10"), bd("100")).gradePoint());
    }

    @Test
    void worksForOtherFullMarks() {
        // ফুল মার্কস ৫০: ৪০/৫০ = ৮০% → A+
        assertEquals("A+", grade("40", "50"));
        // ৩৯.৯/৫০ = ৭৯.৮% → A
        assertEquals("A", grade("39.9", "50"));
        // ১৬.৫/৫০ = ৩৩% → D, ১৬.৪/৫০ = ৩২.৮% → F
        assertEquals("D", grade("16.5", "50"));
        assertEquals("F", grade("16.4", "50"));
        // ফুল মার্কস ২০০ নয়, ৭: ৫.৬/৭ = ৮০% → A+
        assertEquals("A+", grade("5.6", "7"));
    }

    @Test
    void veryCloseToBoundaryIsNotRoundedUp() {
        // ৭৯.৯৯৯৯ কখনো ৮০ হয়ে A+ হবে না
        assertEquals("A", grade("79.9999", "100"));
    }

    @Test
    void bandsOrderInTheListDoesNotMatter() {
        List<GradeBand> shuffled = List.of(scale.get(3), scale.get(6), scale.get(0), scale.get(5),
                scale.get(2), scale.get(4), scale.get(1));
        assertEquals("A", GradeCalculator.lookup(shuffled, bd("75"), bd("100")).grade());
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> grade("-1", "100"));
        assertThrows(IllegalArgumentException.class, () -> grade("101", "100"));
        assertThrows(IllegalArgumentException.class, () -> grade("10", "0"));
        assertThrows(IllegalArgumentException.class, () -> grade("10", "-5"));
    }

    @Test
    void scaleWithoutZeroBandFailsLoudly() {
        List<GradeBand> broken = List.of(new GradeBand(bd("50"), "P", bd("1.00")));
        assertThrows(IllegalStateException.class, () -> GradeCalculator.lookup(broken, bd("10"), bd("100")));
    }
}