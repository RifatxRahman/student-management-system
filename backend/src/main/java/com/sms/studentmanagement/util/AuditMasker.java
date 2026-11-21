package com.sms.studentmanagement.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AuditMasker {

    private static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s+([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_FIELD = Pattern.compile("(?i)(\"?(?:password|temporaryPassword|secret|newPassword|currentPassword)\"?\\s*[:=]\\s*\"?)([^\",\\s}]+)(\"?)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?88)?01[3-9]\\d{8}");

    private AuditMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // 1. Bearer Token মাস্কিং
        Matcher tokenMatcher = BEARER_TOKEN.matcher(result);
        if (tokenMatcher.find()) {
            result = tokenMatcher.replaceAll("Bearer [PROTECTED_TOKEN]");
        }

        // 2. পাসওয়ার্ড ও সিক্রেট ফিল্ড মাস্কিং
        Matcher pwdMatcher = PASSWORD_FIELD.matcher(result);
        if (pwdMatcher.find()) {
            result = pwdMatcher.replaceAll("$1***PROTECTED***$3");
        }

        return result;
    }

    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }
}
