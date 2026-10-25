package com.ecommerce.backend.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

/** Only server-managed claims grant administration; user_metadata is never trusted. */
public final class AdminPermissions {
    private static final Logger log = LoggerFactory.getLogger(AdminPermissions.class);

    private AdminPermissions() {}

    public static boolean allowed(Jwt jwt) {
        if (jwt == null) {
            log.warn("[ADMIN-AUTH] JWT is null");
            return false;
        }
        Object metadata = jwt.getClaims().get("app_metadata");
        boolean isAdmin = metadata instanceof Map<?, ?> values && "admin".equals(values.get("role"));
        if (!isAdmin) {
            log.warn("[ADMIN-AUTH] User {} is not admin. app_metadata: {}, claims: {}",
                    jwt.getSubject(), metadata, jwt.getClaims().keySet());
        } else {
            log.info("[ADMIN-AUTH] User {} successfully verified as admin", jwt.getSubject());
        }
        return isAdmin;
    }
}
