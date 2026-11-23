package com.sms.studentmanagement.security;

import com.sms.studentmanagement.exception.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * লগইন ও সংবেদনশীল এন্ডপয়েন্টে ব্রুট-ফোর্স বা ডিনায়াল অফ সার্ভিস (DoS) রোধ করার জন্য স্লাইডিং-উইন্ডো রেট লিমিটার।
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final long WINDOW_SECONDS = 60; // ১ মিনিট

    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    public void checkRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - WINDOW_SECONDS;

        requestCounts.compute(clientIp, (key, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }

            // পুরনো এক্সপায়ার্ড টাইমস্ট্যাম্প অপসারণ
            Iterator<Long> it = timestamps.iterator();
            while (it.hasNext()) {
                if (it.next() < windowStart) {
                    it.remove();
                }
            }

            if (timestamps.size() >= MAX_ATTEMPTS_PER_WINDOW) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "খুব ঘনঘন অনুরোধ পাঠানো হয়েছে। অনুগ্রহ করে ১ মিনিট পর আবার চেষ্টা করুন।");
            }

            timestamps.add(now);
            return timestamps;
        });
    }

    public void reset(String clientIp) {
        if (clientIp != null) {
            requestCounts.remove(clientIp);
        }
    }

    public void cleanupOldEntries() {
        long windowStart = Instant.now().getEpochSecond() - WINDOW_SECONDS;
        List<String> keys = new ArrayList<>(requestCounts.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String ip = keys.get(i);
            List<Long> times = requestCounts.get(ip);
            if (times == null || times.isEmpty()) {
                requestCounts.remove(ip);
            } else {
                synchronized (times) {
                    times.removeIf(t -> t < windowStart);
                    if (times.isEmpty()) {
                        requestCounts.remove(ip);
                    }
                }
            }
        }
    }
}
