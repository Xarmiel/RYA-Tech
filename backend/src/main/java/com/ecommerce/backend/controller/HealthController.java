package com.ecommerce.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de verificación de salud ligero y seguro para plataformas de despliegue (Render).
 * Responde 200 OK con {"status": "UP"} sin exponer metadatos, esquemas ni credenciales.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Comprobación ligera de disponibilidad del servicio")
public class HealthController {

    @GetMapping
    @Operation(summary = "Comprobación de salud", description = "Retorna el estado operativo del backend sin exponer datos sensibles.")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
