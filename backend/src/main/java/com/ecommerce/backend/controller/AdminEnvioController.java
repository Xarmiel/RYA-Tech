package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.TarifaEnvioRequest;
import com.ecommerce.backend.entity.TarifaEnvio;
import com.ecommerce.backend.service.EnvioAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;
import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/envios")
public class AdminEnvioController {
    private final EnvioAdminService service;

    @GetMapping
    public List<TarifaEnvio> listar() {
        return service.listar();
    }

    @PostMapping
    public TarifaEnvio crear(@Valid @RequestBody TarifaEnvioRequest data) {
        return service.guardar(null, data);
    }

    @PutMapping("/{id}")
    public TarifaEnvio editar(@PathVariable UUID id, @Valid @RequestBody TarifaEnvioRequest data) {
        return service.guardar(id, data);
    }

    @GetMapping("/pedidos")
    public Map<String, Object> pedidos(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.pedidos(page);
    }

    public record Seguimiento(
            @NotBlank @Pattern(regexp = "PENDIENTE|PREPARANDO|ENVIADO|ENTREGADO|LISTO_RECOJO|RETIRADO") String estado,
            @Size(max = 160) String transportista, @Size(max = 100) String codigo, @NotNull OffsetDateTime version) {
    }

    @PutMapping("/pedidos/{id}/seguimiento")
    public void seguimiento(@PathVariable UUID id, @Valid @RequestBody Seguimiento data) {
        service.seguimiento(id, data.estado(), data.transportista(), data.codigo(), data.version());
    }
}
