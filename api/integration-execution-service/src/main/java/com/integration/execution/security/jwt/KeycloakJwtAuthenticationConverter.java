package com.integration.execution.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Extract roles from realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object realmRolesObj = realmAccess.get("roles");
            if (realmRolesObj instanceof Collection<?> realmRoles) {
                realmRoles.forEach(role -> roles.add(String.valueOf(role)));
            }
        }

        // Extract roles from resource_access.account.roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object accountObj = resourceAccess.get("account");
            if (accountObj instanceof Map<?, ?> account) {
                Object accountRolesObj = account.get("roles");
                if (accountRolesObj instanceof Collection<?> accountRoles) {
                    accountRoles.forEach(role -> roles.add(String.valueOf(role)));
                }
            }
        }

        // Extract from custom "roles" claim
        Object customRolesObj = jwt.getClaim("roles");
        if (customRolesObj instanceof Collection<?> customRoles) {
            customRoles.forEach(role -> roles.add(String.valueOf(role)));
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
