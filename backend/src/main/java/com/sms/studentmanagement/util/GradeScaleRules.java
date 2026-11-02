package com.sms.studentmanagement.util;

import com.sms.studentmanagement.exception.ApiException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class GradeScaleRules {

    private GradeScaleRules() {
    }

    /** ভুল হলে 400। নিয়ম: কমপক্ষে ২টা গ্রেড, ০% থেকে শুরু, কোনো ফাঁক/ডুপ্লিকেট নয়, উঁচু মার্কসে পয়েন্ট কম নয়। */
    public static void validate(List<GradeBand> bands) {
        if (bands.size() < 2) {
            throw bad("কমপক্ষে ২টা গ্রেড লাগবে");
        }
        List<GradeBand> sorted = bands.stream()
                .sorted(Comparator.comparing(GradeBand::minPercent))
                .toList();

        if (sorted.get(0).minPercent().compareTo(BigDecimal.ZERO) != 0) {
            throw bad("সবচেয়ে নিচের গ্রেড ০% থেকে শুরু হতে হবে, নইলে কিছু নম্বরের গ্রেড থাকবে না");
        }

        Set<String> names = new HashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            GradeBand band = sorted.get(i);
            if (!names.add(band.grade().trim().toLowerCase(Locale.ROOT))) {
                throw bad("একই গ্রেডের নাম দুবার আছে: " + band.grade());
            }
            if (i > 0) {
                GradeBand lower = sorted.get(i - 1);
                if (band.minPercent().compareTo(lower.minPercent()) == 0) {
                    throw bad("দুটো গ্রেডের শুরু একই: " + band.minPercent() + "%");
                }
                if (band.gradePoint().compareTo(lower.gradePoint()) < 0) {
                    throw bad("উঁচু নম্বরের গ্রেড পয়েন্ট নিচেরটার চেয়ে কম হতে পারে না: " + band.grade());
                }
            }
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}