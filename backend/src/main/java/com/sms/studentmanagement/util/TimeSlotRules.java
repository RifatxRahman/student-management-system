package com.sms.studentmanagement.util;

import com.sms.studentmanagement.exception.ApiException;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;

public final class TimeSlotRules {

    private TimeSlotRules() {
    }

    /** দুটো সময়সীমা মিশে যায় কি না। ঠিক ছুঁয়ে থাকলে (৯:৪৫ শেষ, ৯:৪৫ শুরু) মিশে যাওয়া নয়। */
    public static boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static void validateRange(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "শেষের সময় শুরুর সময়ের পরে হতে হবে");
        }
    }
}