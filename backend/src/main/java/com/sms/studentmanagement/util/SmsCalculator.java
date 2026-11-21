package com.sms.studentmanagement.util;

import java.math.BigDecimal;

public final class SmsCalculator {

    private static final int GSM_SINGLE_MAX = 160;
    private static final int GSM_MULTI_MAX = 153;

    private static final int UNICODE_SINGLE_MAX = 70;
    private static final int UNICODE_MULTI_MAX = 67;

    private SmsCalculator() {
    }

    /** কোনো টেক্সটে বাংলা অক্ষর (Unicode Range \u0980-\u09FF) আছে কি না */
    public static boolean isUnicode(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0980' && c <= '\u09FF') {
                return true;
            }
        }
        return false;
    }

    /** টেক্সটের ওপর ভিত্তি করে কয়টি SMS পার্ট কাটবে তা হিসাব করে */
    public static int calculateParts(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int length = text.length();
        boolean unicode = isUnicode(text);

        if (unicode) {
            if (length <= UNICODE_SINGLE_MAX) {
                return 1;
            }
            return (int) Math.ceil((double) length / UNICODE_MULTI_MAX);
        } else {
            if (length <= GSM_SINGLE_MAX) {
                return 1;
            }
            return (int) Math.ceil((double) length / GSM_MULTI_MAX);
        }
    }

    /** মোট খরচ হিসাব (টাকার হিসাবে সর্বদা BigDecimal) */
    public static BigDecimal calculateCost(int parts, BigDecimal ratePerSms) {
        if (parts <= 0 || ratePerSms == null) {
            return BigDecimal.ZERO;
        }
        return ratePerSms.multiply(BigDecimal.valueOf(parts));
    }
}
