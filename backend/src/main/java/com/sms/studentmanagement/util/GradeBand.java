package com.sms.studentmanagement.util;

import java.math.BigDecimal;

/** একটা গ্রেড: minPercent% বা তার বেশি হলে এই গ্রেড, পরের উঁচু গ্রেডের শুরুর আগ পর্যন্ত। */
public record GradeBand(BigDecimal minPercent, String grade, BigDecimal gradePoint) {
}