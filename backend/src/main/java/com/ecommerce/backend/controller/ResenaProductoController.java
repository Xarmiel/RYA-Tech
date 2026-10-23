package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ResenasProductoDto;
import com.ecommerce.backend.service.ResenaProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/productos/{productoId}/resenas")
@RequiredArgsConstructor
@Tag(name = "Reseñas", description = "Reseñas públicas y verificadas de productos")
public class ResenaProductoController {
    private final ResenaProductoService service;

    @GetMapping
    @Operation(summary = "Listar reseñas publicadas de un producto")
    public ResponseEntity<ResenasProductoDto> listar(@PathVariable UUID productoId) {
        return ResponseEntity.ok(service.listarPublicadas(productoId));
    }
}
