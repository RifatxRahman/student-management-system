package com.sms.studentmanagement.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sms")
@Getter
@Setter
public class SmsProperties {

    /** true হলে আসল গেটওয়ে কল করবে, false হলে কনসোলে ডেমো লগ করবে */
    private boolean enabled = false;

    /** কোন প্রোভাইডার: greenweb, bulksmsbd, ইত্যাদি */
    private String provider = "greenweb";

    private String apiUrl = "http://api.greenweb.com.bd/api.php";
    private String token = ""; // গেটওয়ে থেকে প্রাপ্ত টোকেন / API Key
    private String senderId = ""; // অনুমোদিত মাস্কিং নাম

    /** ডিফল্ট ভাষা: BN (বাংলা) অথবা EN (ইংরেজি) */
    private String defaultLanguage = "BN";

    /** প্রতি SMS পার্টের খরচ (ডিফল্ট: ০.২৫ টাকা) */
    private BigDecimal costPerSms = new BigDecimal("0.25");

    /** স্বয়ংক্রিয় শিডিউলার সক্রিয় কি না */
    private boolean schedulerEnabled = true;
}
