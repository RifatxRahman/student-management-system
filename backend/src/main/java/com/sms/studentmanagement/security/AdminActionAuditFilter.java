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

/**
 * /api/admin/*-এ মিউটেটিং অনুরোধগুলো পরিমাপ করে এবং Audit Log-এ সংরক্ষণ করে।
 * শরীরের (body) সংবেদনশীল তথ্য স্পর্শ করে না।
 */
public class AdminActionAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminActionAuditFilter.class);

    private static final String ADMIN_PREFIX = "/api/admin";
    private static final Pattern USER_ID = Pattern.compile("^/api/admin/users/(\\d+)(/.*)?$");
    private static final Pattern RBAC_USER_ID = Pattern.compile("^/api/admin/rbac/users/(\\d+)(/.*)?$");

    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS");

    private final AuditService auditService;

    public AdminActionAuditFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String path = request.getRequestURI();
            int statusCode = response.getStatus();

            if (!READ_ONLY.contains(request.getMethod()) && path.startsWith(ADMIN_PREFIX)) {
                try {
                    String action = "ADMIN_ACTION";
                    String entityType = "SYSTEM";
                    Long entityId = null;

                    Matcher userMatcher = USER_ID.matcher(path);
                    Matcher rbacUserMatcher = RBAC_USER_ID.matcher(path);

                    if (userMatcher.matches()) {
                        action = "ADMIN_USER_ACTION";
                        entityType = "USER";
                        entityId = Long.valueOf(userMatcher.group(1));
                    } else if (rbacUserMatcher.matches()) {
                        action = "RBAC_USER_OVERRIDE";
                        entityType = "USER_PERMISSIONS";
                        entityId = Long.valueOf(rbacUserMatcher.group(1));
                    } else if (path.startsWith("/api/admin/rbac/roles")) {
                        action = "RBAC_ROLE_MATRIX_UPDATE";
                        entityType = "ROLE";
                    }

                    String status = (statusCode >= 200 && statusCode < 300) ? "SUCCESS" : "FAILED";
                    String details = request.getMethod() + " " + path + " [HTTP " + statusCode + ", " + duration + "ms]";

                    auditService.log(action, entityType, entityId, details, status, duration);
                } catch (RuntimeException e) {
                    log.warn("Audit log সংরক্ষণ করা সম্ভব হয়নি: {}", e.getMessage());
                }
            }
        }
    }
}
