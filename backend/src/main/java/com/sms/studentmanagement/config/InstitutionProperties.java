package com.sms.studentmanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** মার্কশিটের মাথার তথ্য। না লিখলে নিচের ডিফল্ট ছাপা হয়। */
@Component
@ConfigurationProperties(prefix = "app.institution")
@Getter
@Setter
public class InstitutionProperties {

    private String name = "আপনার প্রতিষ্ঠানের নাম";
    private String address = "";
    private String phone = "";
}