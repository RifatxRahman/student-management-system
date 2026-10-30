package com.sms.studentmanagement.util;

import com.sms.studentmanagement.exception.ApiException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class PhoneNumbers {

    private static final Pattern BD_MOBILE = Pattern.compile("^01[3-9][0-9]{8}$");

    private PhoneNumbers() {
    }

    /**
     * "+8801712345678", "8801712345678", "017-1234-5678", "০১৭১২৩৪৫৬৭৮"
     * সবই "01712345678" হয়ে যায়। খালি হলে null ফেরে।
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : raw.trim().toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(Character.forDigit(Character.digit(c, 10), 10));
            } else if (c == '+' && digits.isEmpty()) {
                digits.append('+');
            } else if (c != ' ' && c != '-' && c != '(' && c != ')') {
                throw invalid();
            }
        }
        String number = digits.toString();
        if (number.startsWith("+880")) {
            number = "0" + number.substring(4);
        } else if (number.startsWith("880")) {
            number = "0" + number.substring(3);
        }
        if (!BD_MOBILE.matcher(number).matches()) {
            throw invalid();
        }
        return number;
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "ফোন নম্বর সঠিক নয় (যেমন: 01712345678)");
    }
}