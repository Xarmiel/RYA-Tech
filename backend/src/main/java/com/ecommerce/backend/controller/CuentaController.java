package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ActualizarPerfilRequestDto;
import com.ecommerce.backend.dto.PerfilUsuarioDto;
import com.ecommerce.backend.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cuenta")
@RequiredArgsConstructor
public class CuentaController {
    private final CuentaService service;

    @GetMapping("/perfil")
    public ResponseEntity<PerfilUsuarioDto> perfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.obtenerOSincronizar(jwt));
    }

    @PutMapping("/perfil")
    public ResponseEntity<PerfilUsuarioDto> actualizar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilRequestDto request) {
        return ResponseEntity.ok(service.actualizar(jwt, request));
    }
}
