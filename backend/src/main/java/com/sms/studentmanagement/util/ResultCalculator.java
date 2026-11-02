package com.sms.studentmanagement.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResultCalculator {

    private static final BigDecimal ZERO_POINT = new BigDecimal("0.00");

    private ResultCalculator() {
    }

    /** entered = মার্কস সারি আছে কি না। absent = অনুপস্থিত। marks = নম্বর (অনুপস্থিত হলে null)। */
    public record SubjectInput(
            Long examSubjectId, BigDecimal fullMarks, BigDecimal passMarks, boolean optional,
            boolean entered, boolean absent, BigDecimal marks) {
    }

    /** counted = false মানে এই বিষয়ে কিছু ধরা হয়নি (ঐচ্ছিক বিষয় শিক্ষার্থী নেয়নি)। */
    public record SubjectResult(
            Long examSubjectId, boolean optional, boolean counted, boolean absent,
            BigDecimal marks, String grade, BigDecimal gradePoint, boolean passed) {
    }

    public record Result(
            List<SubjectResult> subjects, BigDecimal totalMarks, BigDecimal totalFullMarks,
            BigDecimal averageMarks, BigDecimal gpa, String grade, boolean passed) {
    }

    /**
     * একজন শিক্ষার্থীর ফল। আবশ্যিক বিষয়ের মার্কস না থাকলে (entered = false) IllegalStateException।
     * ঐচ্ছিক বিষয়ে মার্কস না থাকলে বা অনুপস্থিত হলে ধরে নেওয়া হয় বিষয়টা নেয়নি।
     */
    public static Result calculate(
            List<SubjectInput> inputs, List<GradeBand> bands, BigDecimal optionalBaseGpa, BigDecimal maxGpa) {
        GradeBand lowest = bands.stream()
                .min(Comparator.comparing(GradeBand::minPercent))
                .orElseThrow(() -> new IllegalStateException("গ্রেড স্কেল খালি"));

        List<SubjectResult> results = new ArrayList<>();
        BigDecimal totalMarks = BigDecimal.ZERO;
        BigDecimal totalFull = BigDecimal.ZERO;
        BigDecimal pointSum = BigDecimal.ZERO;
        BigDecimal bestBonus = BigDecimal.ZERO;
        int compulsory = 0;
        int counted = 0;
        boolean allPassed = true;

        for (SubjectInput in : inputs) {
            if (!in.entered() && !in.optional()) {
                throw new IllegalStateException("আবশ্যিক বিষয়ের মার্কস এন্ট্রি বাকি");
            }
            boolean taken = in.entered() && !(in.optional() && in.absent());
            if (!taken) {
                results.add(new SubjectResult(in.examSubjectId(), in.optional(), false, false, null, null, null, true));
                continue;
            }

            BigDecimal marks = in.absent() ? null : in.marks();
            boolean passed = !in.absent() && marks.compareTo(in.passMarks()) >= 0;
            String grade;
            BigDecimal point;
            if (passed) {
                GradeCalculator.Result found = GradeCalculator.lookup(bands, marks, in.fullMarks());
                grade = found.grade();
                point = found.gradePoint();
            } else {
                grade = lowest.grade();
                point = ZERO_POINT;
            }

            totalMarks = totalMarks.add(marks == null ? BigDecimal.ZERO : marks);
            totalFull = totalFull.add(in.fullMarks());
            counted++;
            if (in.optional()) {
                if (passed) {
                    bestBonus = bestBonus.max(point.subtract(optionalBaseGpa).max(BigDecimal.ZERO));
                }
            } else {
                compulsory++;
                pointSum = pointSum.add(point);
                if (!passed) {
                    allPassed = false;
                }
            }
            results.add(new SubjectResult(in.examSubjectId(), in.optional(), true, in.absent(), marks, grade, point, passed));
        }

        if (compulsory == 0) {
            throw new IllegalStateException("কমপক্ষে একটা আবশ্যিক বিষয় লাগবে");
        }

        BigDecimal gpa;
        if (allPassed) {
            BigDecimal raw = pointSum.add(bestBonus).divide(BigDecimal.valueOf(compulsory), 10, RoundingMode.HALF_UP);
            gpa = raw.min(maxGpa).setScale(2, RoundingMode.HALF_UP);
        } else {
            gpa = ZERO_POINT;
        }
        String grade = allPassed ? overallGrade(bands, gpa) : lowest.grade();
        BigDecimal average = totalMarks.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);

        return new Result(
                results, totalMarks.setScale(2, RoundingMode.HALF_UP), totalFull.setScale(2, RoundingMode.HALF_UP),
                average, gpa, grade, allPassed);
    }

    /** GPA-র সমান বা ঠিক নিচের গ্রেড পয়েন্টওয়ালা গ্রেড (যেমন GPA ৪.৮৩ ➜ ৪.০০ = A)। */
    public static String overallGrade(List<GradeBand> bands, BigDecimal gpa) {
        return bands.stream()
                .filter(b -> b.gradePoint().compareTo(gpa) <= 0)
                .max(Comparator.comparing(GradeBand::gradePoint).thenComparing(GradeBand::minPercent))
                .map(GradeBand::grade)
                .orElseGet(() -> bands.stream()
                        .min(Comparator.comparing(GradeBand::minPercent))
                        .map(GradeBand::grade)
                        .orElseThrow());
    }
}