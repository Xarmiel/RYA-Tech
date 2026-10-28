package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.AdminOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/operacion")
public class AdminOperationsController {
    private final AdminOperationsService service;

    public record Reason(@NotBlank @Size(min = 5, max = 2000) String motivo) {
    }

    public record Moderation(@NotBlank @Pattern(regexp = "APROBADA|OCULTA|RECHAZADA|PENDIENTE") String estado,
            @NotBlank @Size(min = 5, max = 2000) String motivo, @Min(0) long version) {
    }

    public record CaseRequest(@NotBlank @Size(max = 40) String pedido,
            @NotBlank @Pattern(regexp = "GARANTIA|DEVOLUCION|INCIDENCIA") String tipo,
            @NotBlank @Size(min = 10, max = 3000) String motivo, @Size(max = 2000) String evidencia) {
    }

    public record Resolution(@NotBlank @Pattern(regexp = "EN_REVISION|APROBADA|RECHAZADA|CERRADA") String estado,
            @NotBlank @Size(min = 10, max = 3000) String resolucion, @Min(0) long version) {
    }

    @GetMapping("/pedidos")
    public Map<String, Object> orders(@RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "") @Size(max = 30) String estado,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.orders(q, estado, page);
    }

    @GetMapping("/pedidos/{id}")
    public Map<String, Object> order(@PathVariable UUID id) {
        return service.order(id);
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public void cancel(@PathVariable UUID id, @Valid @RequestBody Reason data, @AuthenticationPrincipal Jwt jwt) {
        service.cancel(id, data.motivo(), UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/resenas")
    public Map<String, Object> reviews(@RequestParam(defaultValue = "") @Size(max = 30) String estado,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.reviews(estado, page);
    }

    @PutMapping("/resenas/{id}")
    public void moderate(@PathVariable UUID id, @Valid @RequestBody Moderation data, @AuthenticationPrincipal Jwt jwt) {
        service.moderate(id, data.estado(), data.motivo(), data.version(), UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/postventa")
    public Map<String, Object> cases(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.cases(page);
    }

    @PostMapping("/postventa")
    public Map<String, UUID> create(@Valid @RequestBody CaseRequest data, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("id", service.createCase(data.pedido(), data.tipo(), data.motivo(), data.evidencia(),
                UUID.fromString(jwt.getSubject())));
    }

    @PutMapping("/postventa/{id}")
    public void resolve(@PathVariable UUID id, @Valid @RequestBody Resolution data, @AuthenticationPrincipal Jwt jwt) {
        service.resolve(id, data.estado(), data.resolucion(), data.version(), UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/reportes")
    public Map<String, Object> reports(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return service.reports(desde, hasta);
    }
}
