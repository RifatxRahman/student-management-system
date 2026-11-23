package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceStatus {
    UNPAID("বকেয়া"),
    PARTIAL("আংশিক পরিশোধিত"),
    PAID("পরিশোধিত"),
    CANCELLED("বাতিল");

    private final String banglaLabel;
}
