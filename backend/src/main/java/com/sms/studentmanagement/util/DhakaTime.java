package com.sms.studentmanagement.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DhakaTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");

    private DhakaTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}