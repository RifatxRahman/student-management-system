package com.sms.studentmanagement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationAudience {
    ALL("সকল শিক্ষার্থী ও অভিভাবক"),
    CLASS("নির্দিষ্ট শ্রেণি"),
    SECTION("নির্দিষ্ট সেকশন"),
    FEE_DEFAULTERS("বকেয়া ফি প্রাপক শিক্ষার্থী");

    private final String banglaLabel;
}
