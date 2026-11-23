package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("অ্যাডমিন / প্রধান শিক্ষক"),
    TEACHER("শিক্ষক"),
    ACCOUNTANT("হিসাবরক্ষক"),
    STAFF("অফিস সহকারী");

    private final String banglaLabel;
}
