package com.sms.studentmanagement.util;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class GradeCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GradeCalculator() {
    }

    public record Result(String grade, BigDecimal gradePoint) {
    }

    /**
     * marks নম্বর (fullMarks-এর মধ্যে) কোন গ্রেডে পড়ে।
     * ভাগ না করে গুণ করে মেলাই (গোল করার ভুলে ৭৯.৯৯৬ যেন ৮০ না হয়ে যায়):
     * marks × ১০০ >= minPercent × fullMarks
     */
    public static Result lookup(List<GradeBand> bands, BigDecimal marks, BigDecimal fullMarks) {
        if (fullMarks == null || fullMarks.signum() <= 0) {
            throw new IllegalArgumentException("ফুল মার্কস ০-র বেশি হতে হবে");
        }
        if (marks == null || marks.signum() < 0 || marks.compareTo(fullMarks) > 0) {
            throw new IllegalArgumentException("নম্বর ০ থেকে ফুল মার্কসের মধ্যে হতে হবে");
        }
        BigDecimal scaledMarks = marks.multiply(HUNDRED);

        List<GradeBand> highestFirst = bands.stream()
                .sorted(Comparator.comparing(GradeBand::minPercent).reversed())
                .toList();
        for (GradeBand band : highestFirst) {
            if (scaledMarks.compareTo(band.minPercent().multiply(fullMarks)) >= 0) {
                return new Result(band.grade(), band.gradePoint());
            }
        }
        throw new IllegalStateException("কোনো গ্রেড মেলেনি, গ্রেড স্কেলে ০% থেকে শুরু হওয়া গ্রেড নেই");
    }
}