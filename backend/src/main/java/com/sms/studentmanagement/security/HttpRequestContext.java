package com.sms.studentmanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpRequestContext {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private HttpRequestContext() {
    }

    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public static String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "127.0.0.1";
        }
        return getClientIpAddress(request);
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        for (int i = 0; i < IP_HEADERS.length; i++) {
            String header = request.getHeader(IP_HEADERS[i]);
            if (header != null && !header.isBlank() && !"unknown".equalsIgnoreCase(header)) {
                // X-Forwarded-For-এ কমা দিয়ে একাধিক আইপি থাকতে পারে (client, proxy1, proxy2)
                int commaIndex = header.indexOf(',');
                if (commaIndex != -1) {
                    return header.substring(0, commaIndex).trim();
                }
                return header.trim();
            }
        }

        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : "127.0.0.1";
    }

    public static String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "Internal/Server";
        }
        return getUserAgent(request);
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "Internal/Server";
        }
        String agent = request.getHeader("User-Agent");
        if (agent == null || agent.isBlank()) {
            return "Unknown";
        }
        return agent.length() > 255 ? agent.substring(0, 255) : agent;
    }
}
