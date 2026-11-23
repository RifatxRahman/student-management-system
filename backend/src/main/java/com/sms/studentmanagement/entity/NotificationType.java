package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    ABSENCE("অনুপস্থিতি বার্তা"),
    FEE_DUE("ফি বকেয়া নোটিশ"),
    PAYMENT_RECEIPT("ফি পরিশোধ রসিদ"),
    EXAM_NOTICE("পরীক্ষার নোটিশ"),
    RESULT_PUBLISH("ফলাফল প্রকাশের নোটিশ"),
    ANNOUNCEMENT("সাধারণ ঘোষণা");

    private final String banglaLabel;
}