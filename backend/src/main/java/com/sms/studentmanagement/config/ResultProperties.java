package com.sms.studentmanagement.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** রেজাল্টের নিয়মের সেটিংস। না লিখলে নিচের ডিফল্ট চলে। */
@Component
@ConfigurationProperties(prefix = "app.result")
@Getter
@Setter
public class ResultProperties {

    /** ঐচ্ছিক বিষয়ের বোনাস = গ্রেড পয়েন্ট − এই সংখ্যা (ধনাত্মক হলে) */
    private BigDecimal optionalBaseGpa = new BigDecimal("2.00");

    /** GPA কখনো এর বেশি হবে না */
    private BigDecimal maxGpa = new BigDecimal("5.00");
}