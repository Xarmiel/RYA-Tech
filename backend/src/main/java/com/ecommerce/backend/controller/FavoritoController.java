package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.SincronizarFavoritosRequestDto;
import com.ecommerce.backend.service.FavoritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favoritos")
@RequiredArgsConstructor
public class FavoritoController {
    private final FavoritoService service;

    @GetMapping
    public List<UUID> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(jwt);
    }

    @PostMapping("/sincronizar")
    public List<UUID> sincronizar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SincronizarFavoritosRequestDto request) {
        return service.sincronizar(jwt, request);
    }
}
