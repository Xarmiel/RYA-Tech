package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carrito")
@RequiredArgsConstructor
@Tag(name = "Carrito", description = "Carrito anónimo persistido y revalidado en backend")
public class CarritoController {
    private static final String SESSION_HEADER = "X-Cart-Session";
    private final CarritoService carritoService;

    @GetMapping
    @Operation(summary = "Consultar el carrito actual")
    public ResponseEntity<CarritoDto> obtener(@RequestHeader(SESSION_HEADER) String sessionKey,
                                               Authentication authentication) {
        return ResponseEntity.ok(carritoService.obtener(sessionKey, usuarioId(authentication)));
    }

    @PostMapping("/items")
    @Operation(summary = "Añadir un producto al carrito")
    public ResponseEntity<CarritoDto> agregar(@RequestHeader(SESSION_HEADER) String sessionKey,
                                               @Valid @RequestBody AgregarItemCarritoRequestDto request,
                                               Authentication authentication) {
        return ResponseEntity.ok(carritoService.agregar(sessionKey, usuarioId(authentication), request));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Cambiar la cantidad de un producto")
    public ResponseEntity<CarritoDto> actualizar(@RequestHeader(SESSION_HEADER) String sessionKey,
                                                  @PathVariable UUID itemId,
                                                  @Valid @RequestBody ActualizarItemCarritoRequestDto request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(carritoService.actualizar(sessionKey, usuarioId(authentication), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Retirar un producto del carrito")
    public ResponseEntity<CarritoDto> eliminar(@RequestHeader(SESSION_HEADER) String sessionKey,
                                                @PathVariable UUID itemId,
                                                Authentication authentication) {
        return ResponseEntity.ok(carritoService.eliminar(sessionKey, usuarioId(authentication), itemId));
    }

    @PostMapping("/vincular")
    @Operation(summary = "Fusionar el carrito anónimo con la cuenta autenticada")
    public ResponseEntity<CarritoDto> vincular(@RequestHeader(SESSION_HEADER) String sessionKey,
                                                Authentication authentication) {
        return ResponseEntity.ok(carritoService.vincular(sessionKey, usuarioId(authentication)));
    }

    private UUID usuarioId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return null;
        return UUID.fromString(jwt.getSubject());
    }
}
