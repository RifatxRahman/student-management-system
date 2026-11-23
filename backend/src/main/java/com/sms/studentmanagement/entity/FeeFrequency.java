package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeeFrequency {
    MONTHLY("মাসিক"),
    TERMLY("টার্ম/সাময়িক"),
    YEARLY("বার্ষিক"),
    ONE_TIME("এককালীন");

    private final String banglaLabel;
}
