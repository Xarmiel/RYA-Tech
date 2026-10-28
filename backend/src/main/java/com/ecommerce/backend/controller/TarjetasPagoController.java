package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.TarjetasPagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/pagos/tarjetas")
@RequiredArgsConstructor
public class TarjetasPagoController {
    private final TarjetasPagoService service;

    public record Save(@NotBlank @Size(max = 512) String token, boolean consentimiento) {
    }

    @GetMapping
    public List<TarjetasPagoService.Card> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping
    public TarjetasPagoService.Card save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Save request) {
        return service.save(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"), request.token(),
                request.consentimiento());
    }
}
