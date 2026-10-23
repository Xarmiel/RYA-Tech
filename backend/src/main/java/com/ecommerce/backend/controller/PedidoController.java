package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.PedidoDto;
import com.ecommerce.backend.service.CuentaService;
import com.ecommerce.backend.service.PedidoService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {
    private final PedidoService service;
    private final CuentaService cuentas;

    @GetMapping
    public PaginatedResponseDto<PedidoDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(100) int size) {
        return service.listar(cuentas.obtenerOSincronizar(jwt).getId(), page, size);
    }

    @GetMapping("/{id}")
    public PedidoDto detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.detalle(cuentas.obtenerOSincronizar(jwt).getId(), id);
    }
}
