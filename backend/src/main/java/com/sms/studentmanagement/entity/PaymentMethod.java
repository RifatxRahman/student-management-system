package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH("নগদ"),
    BKASH("বিকাশ"),
    NAGAD("নগদ (MFS)"),
    ROCKET("রকেট"),
    BANK("ব্যাংক ডিপোজিট"),
    CARD("কার্ড");

    private final String banglaLabel;
}
