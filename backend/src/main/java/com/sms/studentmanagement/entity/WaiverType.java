package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WaiverType {
    PERCENTAGE("শতকরা (%)"),
    FIXED_AMOUNT("নির্দিষ্ট টাকা (৳)");

    private final String banglaLabel;
}
