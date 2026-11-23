package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentGatewayProvider {
    BKASH("বিকাশ মার্চেন্ট গেটওয়ে (bKash)"),
    SSLCOMMERZ("SSLCommerz গেটওয়ে"),
    NAGAD("নগদ ডিরেক্ট পে"),
    MOCK("স্যান্ডবক্স সিমুলেটর");

    private final String label;
}
