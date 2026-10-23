package com.ecommerce.backend.config;

import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

/** Only server-managed claims grant administration; user_metadata is never trusted. */
public final class AdminPermissions {
    private AdminPermissions() {}
    public static boolean allowed(Jwt jwt) {
        if (jwt == null) return false;
        Object metadata = jwt.getClaims().get("app_metadata");
        return metadata instanceof Map<?, ?> values && "admin".equals(values.get("role"));
    }
}
