package com.sms.studentmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** ডেমো: শুধু লগে লেখে, আসল SMS পাঠায় না। Phase 5-এ আসল Gateway-র ক্লাস এর জায়গা নেবে। */
@Component
public class DemoSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(DemoSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[DEMO SMS] to {} ({} chars). No real SMS was sent.", mask(phone), message.length());
    }

    @Override
    public boolean isDemo() {
        return true;
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}