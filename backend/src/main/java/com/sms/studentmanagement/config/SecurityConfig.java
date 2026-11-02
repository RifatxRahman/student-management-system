package com.sms.studentmanagement.config;

import com.sms.studentmanagement.repository.UserRepository;
import com.sms.studentmanagement.security.AdminActionAuditFilter;
import com.sms.studentmanagement.security.CurrentUserCheckFilter;
import com.sms.studentmanagement.service.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, UserRepository userRepository, AuditService auditService)
            throws Exception {
        http
                // টোকেন Authorization হেডারে যায় (কুকিতে নয়), তাই সাধারণ CSRF দরকার নেই।
                // কুকি-ভিত্তিক /refresh ও /logout-কে SameSite=Strict রক্ষা করছে।
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ১) সবার জন্য খোলা
                        .requestMatchers("/api/ping").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // ২) লগইন করা যে কেউ
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        // ৩) শিক্ষক শুধু নিজের তথ্য দেখতে পারেন
                        .requestMatchers("/api/teachers/me/**").authenticated()
                        // ৪) হাজিরা: Admin ও Teacher (কোন সেকশন, সেটা Service যাচাই করে)
                        .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "TEACHER")
                        // ৪ক) মার্কস এন্ট্রি: Admin ও Teacher (কোন সেকশন-বিষয়, সেটা Service যাচাই করে)
                        .requestMatchers("/api/marks/**").hasAnyRole("ADMIN", "TEACHER")
                        // ৪খ) রেজাল্ট প্রিভিউ ও প্রকাশ: শুধু Admin। প্রকাশিত ফল: Admin ও Teacher (নিজের সেকশন)
                        .requestMatchers("/api/exams/*/preview", "/api/exams/*/publish").hasRole("ADMIN")
                        .requestMatchers("/api/exams/*/results").hasAnyRole("ADMIN", "TEACHER")
                        // ৫) ব্যক্তিগত তথ্য: শুধু Admin (পড়া ও বদলানো দুটোই)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/students/**", "/api/guardians/**", "/api/teachers/**", "/api/enrollments/**")
                        .hasRole("ADMIN")
                        // ৬) সাধারণ তথ্য পড়া (সেশন, ক্লাস, বিষয়, ছুটি): লগইন করা যে কেউ
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // ৭) বাকি সব (বদলানো, যেমন ছুটি যোগ/মোছা): ডিফল্টভাবে শুধু Admin
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(new CurrentUserCheckFilter(userRepository), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AdminActionAuditFilter(auditService), CurrentUserCheckFilter.class);
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}