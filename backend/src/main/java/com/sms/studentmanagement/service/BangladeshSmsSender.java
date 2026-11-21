package com.sms.studentmanagement.service;

import com.sms.studentmanagement.config.SmsProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class BangladeshSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(BangladeshSmsSender.class);
    private final SmsProperties properties;
    private final RestClient restClient = RestClient.create();

    @Override
    public void send(String phone, String message) {
        String normalizedPhone = normalizePhone(phone);
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = String.format("%s?token=%s&to=%s&message=%s",
                    properties.getApiUrl(),
                    properties.getToken(),
                    normalizedPhone,
                    encodedMessage);

            log.info("Sending SMS to {} via {}", mask(normalizedPhone), properties.getProvider());

            String response = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String.class);

            if (response == null || (!response.toLowerCase().contains("ok") && !response.toLowerCase().contains("success"))) {
                throw new RuntimeException("গেটওয়ে রেসপন্স ত্রুটি: " + response);
            }

            log.info("SMS successfully sent to {}", mask(normalizedPhone));
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", mask(normalizedPhone), e.getMessage());
            throw new RuntimeException("SMS গেটওয়ে ত্রুটি: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isDemo() {
        return false;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String p = phone.trim().replaceAll("[^0-9]", "");
        if (p.startsWith("880")) return p;
        if (p.startsWith("0")) return "88" + p;
        return "880" + p;
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
