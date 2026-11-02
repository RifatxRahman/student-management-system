package com.sms.studentmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sms.studentmanagement.util.ResultCalculator.Result;
import com.sms.studentmanagement.util.ResultCalculator.SubjectInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNum(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private final List<GradeBand> scale = List.of(
            new GradeBand(bd("80"), "A+", bd("5.00")),
            new GradeBand(bd("70"), "A", bd("4.00")),
            new GradeBand(bd("60"), "A-", bd("3.50")),
            new GradeBand(bd("50"), "B", bd("3.00")),
            new GradeBand(bd("40"), "C", bd("2.00")),
            new GradeBand(bd("33"), "D", bd("1.00")),
            new GradeBand(bd("0"), "F", bd("0.00")));

    private long nextId = 1;

    /** আবশ্যিক বিষয়: ফুল ১০০, পাস ৩৩ */
    private SubjectInput sub(String marks) {
        return new SubjectInput(nextId++, bd("100"), bd("33"), false, true, false, bd(marks));
    }

    private SubjectInput sub(String full, String pass, String marks) {
        return new SubjectInput(nextId++, bd(full), bd(pass), false, true, false, bd(marks));
    }

    private SubjectInput absent() {
        return new SubjectInput(nextId++, bd("100"), bd("33"), false, true, true, null);
    }

    private SubjectInput optional(String marks) {
        return new SubjectInput(nextId++, bd("100"), bd("33"), true, true, false, bd(marks));
    }

    private SubjectInput optionalNotTaken() {
        return new SubjectInput(nextId++, bd("100"), bd("33"), true, false, false, null);
    }

    private Result calc(SubjectInput... inputs) {
        return ResultCalculator.calculate(List.of(inputs), scale, bd("2.00"), bd("5.00"));
    }

    @Test
    void allTopMarksGivesFiveAndAPlus() {
        Result result = calc(sub("90"), sub("85"), sub("100"), sub("80"));

        assertNum("5.00", result.gpa());
        assertEquals("A+", result.grade());
        assertTrue(result.passed());
    }

    @Test
    void gpaIsAverageOfPointsRoundedHalfUp() {
        // ৫ + ৪ + ৩.৫ + ৩ = ১৫.৫ ÷ ৪ = ৩.৮৭৫ → ৩.৮৮
        Result result = calc(sub("85"), sub("72"), sub("65"), sub("55"));

        assertNum("3.88", result.gpa());
        // ৩.৮৮ ≥ ৩.৫০ (A-) কিন্তু < ৪.০০ (A)
        assertEquals("A-", result.grade());
    }

    @Test
    void gpaRepeatingDecimalIsRoundedToTwoPlaces() {
        // (৫ + ৫ + ৪) ÷ ৩ = ৪.৬৬৬… → ৪.৬৭
        Result result = calc(sub("90"), sub("95"), sub("75"));
        assertNum("4.67", result.gpa());
        assertEquals("A", result.grade());
    }

    @Test
    void failingOneCompulsorySubjectFailsEverything() {
        Result result = calc(sub("90"), sub("32.99"), sub("85"));

        assertFalse(result.passed());
        assertNum("0.00", result.gpa());
        assertEquals("F", result.grade());
        // ফেল হলেও মোট নম্বর হিসাব হয়
        assertNum("207.99", result.totalMarks());
    }

    @Test
    void exactlyPassMarksPassesWithDGrade() {
        Result result = calc(sub("33"), sub("33"));

        assertTrue(result.passed());
        assertNum("1.00", result.gpa());
        assertEquals("D", result.grade());
    }

    @Test
    void absentInCompulsorySubjectIsFail() {
        Result result = calc(sub("90"), absent());

        assertFalse(result.passed());
        assertNum("0.00", result.gpa());
        assertNum("90.00", result.totalMarks());
        assertNum("200.00", result.totalFullMarks());
    }

    @Test
    void optionalSubjectAddsBonusAboveBaseGpa() {
        // চারটা আবশ্যিক বিষয়ে ৪.০০ করে = ১৬, ঐচ্ছিকে ৮৫ (৫.০০) → বোনাস ৩.০০ → (১৬ + ৩) ÷ ৪ = ৪.৭৫
        Result result = calc(sub("75"), sub("72"), sub("70"), sub("78"), optional("85"));

        assertNum("4.75", result.gpa());
    }

    @Test
    void optionalSubjectWithPointAtOrBelowBaseAddsNothing() {
        // ঐচ্ছিকে ৪৫ (C = ২.০০) বা ৩৫ (D = ১.০০): বোনাস নেই
        assertNum("4.00", calc(sub("75"), sub("72"), optional("45")).gpa());
        assertNum("4.00", calc(sub("75"), sub("72"), optional("35")).gpa());
    }

    @Test
    void failedOrMissingOptionalSubjectDoesNotFailTheResult() {
        Result failedOptional = calc(sub("75"), sub("72"), optional("20"));
        assertTrue(failedOptional.passed());
        assertNum("4.00", failedOptional.gpa());

        Result notTaken = calc(sub("75"), sub("72"), optionalNotTaken());
        assertTrue(notTaken.passed());
        assertNum("4.00", notTaken.gpa());
        assertNum("200.00", notTaken.totalFullMarks());
    }

    @Test
    void absentInOptionalSubjectIsTreatedAsNotTaken() {
        SubjectInput absentOptional = new SubjectInput(99L, bd("100"), bd("33"), true, true, true, null);
        Result result = calc(sub("75"), sub("72"), absentOptional);

        assertTrue(result.passed());
        assertNum("4.00", result.gpa());
        assertNum("147.00", result.totalMarks());
    }

    @Test
    void gpaNeverExceedsFive() {
        // সব আবশ্যিকে ৫.০০, ঐচ্ছিকে ৫.০০ (বোনাস ৩.০০) → ৫.৭৫ → সীমা ৫.০০
        Result result = calc(sub("90"), sub("95"), optional("90"));

        assertNum("5.00", result.gpa());
        assertEquals("A+", result.grade());
    }

    @Test
    void optionalMarksAreIncludedInTotalAndAverage() {
        Result result = calc(sub("80"), sub("60"), optional("70"));

        assertNum("210.00", result.totalMarks());
        assertNum("300.00", result.totalFullMarks());
        assertNum("70.00", result.averageMarks());
    }

    @Test
    void gradesUsePercentageSoOtherFullMarksWork() {
        // ফুল ৫০, পাস ১৭: ৪০/৫০ = ৮০% → A+, ৩৯.৯/৫০ = ৭৯.৮% → A
        Result top = calc(sub("50", "17", "40"), sub("100", "33", "90"));
        assertNum("5.00", top.gpa());

        Result nearTop = calc(sub("50", "17", "39.9"), sub("100", "33", "90"));
        // (৪.০০ + ৫.০০) ÷ ২ = ৪.৫০
        assertNum("4.50", nearTop.gpa());
    }

    @Test
    void missingCompulsoryMarksFailsLoudly() {
        SubjectInput missing = new SubjectInput(5L, bd("100"), bd("33"), false, false, false, null);
        assertThrows(IllegalStateException.class, () -> calc(sub("80"), missing));
    }

    @Test
    void examWithoutCompulsorySubjectFailsLoudly() {
        assertThrows(IllegalStateException.class, () -> calc(optional("80")));
    }

    @Test
    void overallGradeFollowsTheScaleNotHardcodedNames() {
        assertEquals("A+", ResultCalculator.overallGrade(scale, bd("5.00")));
        assertEquals("A", ResultCalculator.overallGrade(scale, bd("4.99")));
        assertEquals("A", ResultCalculator.overallGrade(scale, bd("4.00")));
        assertEquals("A-", ResultCalculator.overallGrade(scale, bd("3.99")));
        assertEquals("B", ResultCalculator.overallGrade(scale, bd("3.00")));
        assertEquals("C", ResultCalculator.overallGrade(scale, bd("2.50")));
        assertEquals("D", ResultCalculator.overallGrade(scale, bd("1.00")));
        assertEquals("F", ResultCalculator.overallGrade(scale, bd("0.99")));
    }

    @Test
    void manySubjectsAreCountedOnce() {
        List<SubjectInput> inputs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            inputs.add(sub("60"));
        }
        Result result = ResultCalculator.calculate(inputs, scale, bd("2.00"), bd("5.00"));
        assertNum("3.50", result.gpa());
        assertNum("480.00", result.totalMarks());
        assertEquals(8, result.subjects().size());
    }
}