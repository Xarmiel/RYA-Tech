package com.ecommerce.backend.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;

class AdminPermissionsTest {
    private Jwt token(String claim, Map<String, String> value) {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject("test-user")
                .claim(claim, value).build();
    }
    @Test void acceptsOnlyServerManagedAdmin() {
        assertTrue(AdminPermissions.allowed(token("app_metadata", Map.of("role", "admin"))));
        assertFalse(AdminPermissions.allowed(token("app_metadata", Map.of("role", "user"))));
        assertFalse(AdminPermissions.allowed(token("user_metadata", Map.of("role", "admin"))));
        assertFalse(AdminPermissions.allowed(token("app_metadata", Map.of())));
        assertFalse(AdminPermissions.allowed(null));
    }
}
