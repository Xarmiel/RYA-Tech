package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.pago.*;
import com.ecommerce.backend.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/iniciar")
    public ResponseEntity<IniciarPagoResponseDto> iniciarPago(
            @RequestHeader(value = "X-Cart-Session", required = false, defaultValue = "") String sessionKey,
            @Valid @RequestBody CrearPedidoPagoRequestDto request,
            Authentication authentication) {
        UUID usuarioId = authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(pagoService.iniciarPago(sessionKey, usuarioId, request));
    }

    @PostMapping("/procesar")
    public ResponseEntity<ResultadoPagoDto> procesarCargo(
            @RequestHeader(value = "X-Cart-Session", required = false, defaultValue = "") String sessionKey,
            @Valid @RequestBody ProcesarCargoRequestDto request,
            Authentication authentication) {
        UUID usuarioId = authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(pagoService.procesarCargo(sessionKey, usuarioId, request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-signature", required = false) String signature) {
        pagoService.procesarWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}

