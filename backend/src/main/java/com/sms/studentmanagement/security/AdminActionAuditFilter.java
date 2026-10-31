package com.sms.studentmanagement.security;

import com.sms.studentmanagement.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/** /api/admin/users-এ সফল বদলানোর অনুরোধ Audit Log-এ লেখে। শরীরের (body) তথ্য কখনো পড়ে না। */
public class AdminActionAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminActionAuditFilter.class);

    // Swagger-এ Admin ব্যবহারকারীর ঠিকানা অন্য হলে শুধু এই দুটো লাইন বদলান
    private static final String PREFIX = "/api/admin/users";
    private static final Pattern USER_ID = Pattern.compile("^/api/admin/users/(\\d+)(/.*)?$");

    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS");

    private final AuditService auditService;

    public AdminActionAuditFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);

        String path = request.getRequestURI();
        int status = response.getStatus();
        if (READ_ONLY.contains(request.getMethod()) || !path.startsWith(PREFIX)
                || status < 200 || status >= 300) {
            return;
        }
        try {
            Matcher matcher = USER_ID.matcher(path);
            Long userId = matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
            auditService.log("ADMIN_USER_ACTION", "USER", userId,
                    request.getMethod() + " " + path + " (" + status + ")");
        } catch (RuntimeException e) {
            // লগ লিখতে না পারলেও মূল কাজ যেন না ভাঙে
            log.warn("Audit log লেখা যায়নি: {}", e.getMessage());
        }
    }
}