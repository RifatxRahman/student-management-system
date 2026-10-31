package com.sms.studentmanagement.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AttendanceMath {

    private AttendanceMath() {
    }

    /**
     * (উপস্থিত + দেরি) ÷ (উপস্থিত + দেরি + অনুপস্থিত) × ১০০, দুই দশমিকে।
     * LEAVE এখানে পাঠাবেন না: ছুটি হিসাবের বাইরে।
     * গোনার মতো কোনো দিনই না থাকলে null (০% নয়, কারণ হিসাবই হয়নি)।
     */
    public static BigDecimal percentage(long present, long late, long absent) {
        long counted = present + late + absent;
        if (counted == 0) {
            return null;
        }
        return BigDecimal.valueOf((present + late) * 100L)
                .divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
    }
}