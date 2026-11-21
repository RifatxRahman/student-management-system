package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationChannel {
    SMS("এসএমএস (SMS)"),
    EMAIL("ইমেইল (Email)"),
    BOTH("উভয় (SMS & Email)");

    private final String banglaLabel;
}
