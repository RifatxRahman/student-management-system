package com.sms.studentmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** অনুরোধকারী কে: তাঁর user id ও তিনি Admin কি না। */
public record Actor(Long userId, boolean admin) {

    public static Actor from(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Actor(Long.valueOf(jwt.getSubject()), admin);
    }
}