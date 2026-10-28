package com.sms.studentmanagement.security;

import com.sms.studentmanagement.entity.User;
import com.sms.studentmanagement.entity.UserStatus;
import com.sms.studentmanagement.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * প্রতিটি অনুরোধে ডেটাবেস থেকে দেখে নেয়:
 * ১) ব্যবহারকারী এখনো সক্রিয় কি না (নিষ্ক্রিয় করলে সাথে সাথে বন্ধ, ১৫ মিনিট অপেক্ষা নয়)
 * ২) রোল বদলে যায়নি তো
 * ৩) প্রথম লগইনে পাসওয়ার্ড বদলানো বাকি থাকলে অন্য সব কাজ বন্ধ
 */
public class CurrentUserCheckFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHEN_MUST_CHANGE =
            Set.of("/api/auth/change-password", "/api/auth/logout");

    private final UserRepository userRepository;

    public CurrentUserCheckFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Long userId = Long.valueOf(jwtAuth.getToken().getSubject());
            Optional<User> found = userRepository.findById(userId);

            if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) {
                reject(response, 401, "Unauthorized", "অ্যাকাউন্ট সক্রিয় নয়");
                return;
            }
            User user = found.get();
            String tokenRole = jwtAuth.getToken().getClaimAsString("role");
            if (!user.getRole().name().equals(tokenRole)) {
                reject(response, 401, "Unauthorized", "আবার লগইন করুন");
                return;
            }
            if (user.isMustChangePassword()
                    && !ALLOWED_WHEN_MUST_CHANGE.contains(request.getRequestURI())) {
                reject(response, 403, "Forbidden", "আগে পাসওয়ার্ড বদলাতে হবে");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status, error, message));
    }
}