package com.sms.studentmanagement.config;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** হাজিরার সেটিংস। না লিখলে নিচের ডিফল্ট চলে। */
@Component
@ConfigurationProperties(prefix = "app.attendance")
@Getter
@Setter
public class AttendanceProperties {

    /** শিক্ষক আজ থেকে কত দিন আগে পর্যন্ত হাজিরা নিতে/বদলাতে পারবেন */
    private int editWindowDays = 3;

    /** সাপ্তাহিক ছুটি (ডিফল্ট শুধু শুক্রবার) */
    private Set<DayOfWeek> weeklyOffDays = EnumSet.of(DayOfWeek.FRIDAY);
}