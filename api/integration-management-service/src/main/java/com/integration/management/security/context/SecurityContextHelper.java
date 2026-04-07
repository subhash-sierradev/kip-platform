package com.integration.management.security.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

@Slf4j
public final class SecurityContextHelper {

    private SecurityContextHelper() {
        // prevent instantiation
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("No authentication or authorities found in security context");
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(r -> r.equals("ROLE_" + role));
    }
}
