package com.workrh.common.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean hasAuthority(String authority) {
        Authentication authentication = currentAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    public static String currentUsername() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return null;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? null : name;
    }

    public static Long currentEmployeeId() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return null;
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Map<?, ?> map)) {
            return null;
        }

        Object employeeId = map.get("employeeId");
        if (employeeId instanceof Number number) {
            return number.longValue();
        }
        if (employeeId instanceof String value && !value.isBlank()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
