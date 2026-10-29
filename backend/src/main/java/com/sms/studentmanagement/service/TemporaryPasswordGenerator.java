package com.sms.studentmanagement.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TemporaryPasswordGenerator {

    // দেখতে এক রকম অক্ষর (0/O, 1/l/I) বাদ দেওয়া হয়েছে, যাতে পড়তে ভুল না হয়
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        String all = LETTERS + DIGITS;
        List<Character> chars = new ArrayList<>();
        chars.add(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        for (int i = 0; i < 10; i++) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder();
        for (char c : chars) {
            password.append(c);
        }
        return password.toString();
    }
}