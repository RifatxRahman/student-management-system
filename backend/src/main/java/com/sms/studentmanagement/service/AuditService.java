package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.AuditLogResponse;
import com.sms.studentmanagement.entity.AuditLog;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.repository.AuditLogRepository;
import com.sms.studentmanagement.repository.UserRepository;
import com.sms.studentmanagement.security.HttpRequestContext;
import com.sms.studentmanagement.util.AuditMasker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String action, String entityType, Long entityId, String details) {
        log(action, entityType, entityId, details, "SUCCESS", 0L);
    }

    public void log(String action, String entityType, Long entityId, String details, String status, long executionTimeMs) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(currentUserId());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details == null ? null : truncate(AuditMasker.mask(details), 1000));
        entry.setIpAddress(HttpRequestContext.getClientIpAddress());
        entry.setUserAgent(HttpRequestContext.getUserAgent());
        entry.setStatus(status != null ? status : "SUCCESS");
        entry.setExecutionTimeMs(executionTimeMs);
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String entityType, Long entityId, int page, int size) {
        return search(entityType, entityId, null, null, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            String entityType,
            Long entityId,
            String action,
            String status,
            Long actorUserId,
            Instant startDate,
            Instant endDate,
            int page,
            int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = auditLogRepository.searchAdvanced(
                blankToNull(entityType),
                entityId,
                blankToNull(action),
                blankToNull(status),
                actorUserId,
                startDate,
                endDate,
                PageRequest.of(Math.max(page, 0), safeSize));

        List<AuditLog> content = result.getContent();
        Set<Long> actorIds = new HashSet<>();
        for (int i = 0; i < content.size(); i++) {
            Long actorId = content.get(i).getActorUserId();
            if (actorId != null) {
                actorIds.add(actorId);
            }
        }

        Map<Long, String> names = new HashMap<>();
        List<User> users = userRepository.findAllById(actorIds);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            names.put(user.getId(), user.getUsername());
        }

        List<AuditLogResponse> items = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            AuditLog a = content.get(i);
            items.add(AuditLogResponse.from(a, names.get(a.getActorUserId())));
        }

        return PageResponse.of(result, items);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Long.valueOf(jwtAuth.getToken().getSubject());
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
