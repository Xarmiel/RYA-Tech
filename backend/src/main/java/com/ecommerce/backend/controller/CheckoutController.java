package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CheckoutOpcionesDto;
import com.ecommerce.backend.dto.CheckoutValidacionDto;
import com.ecommerce.backend.dto.ValidarCheckoutRequestDto;
import com.ecommerce.backend.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService service;

    @GetMapping("/opciones")
    public ResponseEntity<CheckoutOpcionesDto> opciones() {
        return ResponseEntity.ok(service.obtenerOpciones());
    }

    @PostMapping("/validar")
    public ResponseEntity<CheckoutValidacionDto> validar(
            @RequestHeader("X-Cart-Session") String sessionKey,
            @Valid @RequestBody ValidarCheckoutRequestDto request,
            Authentication authentication) {
        UUID usuarioId = authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(service.validar(sessionKey, usuarioId, request));
    }
}
