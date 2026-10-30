package com.sms.studentmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentCodeGenerator {

    private final JdbcTemplate jdbcTemplate;

    /**
     * বছর অনুযায়ী ক্রমিক ID: 2026-00001, 2026-00002 ...
     * অবশ্যই কোনো @Transactional কাজের ভেতর থেকে ডাকতে হবে
     * (LAST_INSERT_ID() একই সংযোগে পড়তে হয়)।
     */
    public String next(int year) {
        String name = "STUDENT-" + year;
        jdbcTemplate.update(
                "INSERT INTO id_sequences (name, counter_value) VALUES (?, LAST_INSERT_ID(1)) "
                        + "ON DUPLICATE KEY UPDATE counter_value = LAST_INSERT_ID(counter_value + 1)",
                name);
        Long value = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return String.format("%d-%05d", year, value);
    }
}