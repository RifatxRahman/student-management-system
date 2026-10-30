package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PageResponse;
import com.sms.studentmanagement.dto.PeopleDtos.AuditLogResponse;
import com.sms.studentmanagement.entity.AuditLog;
import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.repository.AuditLogRepository;
import com.sms.studentmanagement.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    /** আপনি কখনো এখানে পাসওয়ার্ড বা গোপন তথ্য পাঠাবেন না। */
    public void log(String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(currentUserId());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details == null ? null : truncate(details, 1000));
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String entityType, Long entityId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = auditLogRepository.search(
                blankToNull(entityType), entityId, PageRequest.of(Math.max(page, 0), safeSize));

        Set<Long> actorIds = result.stream()
                .map(AuditLog::getActorUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(actorIds)) {
            names.put(user.getId(), user.getUsername());
        }

        List<AuditLogResponse> items = result.stream()
                .map(a -> AuditLogResponse.from(a, names.get(a.getActorUserId())))
                .toList();
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