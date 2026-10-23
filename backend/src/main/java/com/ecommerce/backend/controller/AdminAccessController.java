package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.AdminPermissions;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAccessController {
    public record Access(boolean allowed, String mode) {}
    @GetMapping("/api/v1/admin/acceso")
    public Access access(@AuthenticationPrincipal Jwt jwt) {
        return new Access(AdminPermissions.allowed(jwt), "draft-only");
    }
}
