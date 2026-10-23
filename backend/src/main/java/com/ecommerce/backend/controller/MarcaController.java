package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CrearMarcaDto;
import com.ecommerce.backend.dto.MarcaDto;
import com.ecommerce.backend.service.MarcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marcas")
@RequiredArgsConstructor
@Tag(name = "Marcas", description = "Operaciones de consulta y gestión sobre marcas")
public class MarcaController {

    private final MarcaService marcaService;

    @GetMapping
    @Operation(summary = "Listar marcas activas", description = "Devuelve todas las marcas activas ordenadas alfabéticamente.")
    public ResponseEntity<List<MarcaDto>> listarMarcas() {
        return ResponseEntity.ok(marcaService.listarActivas());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Obtener marca por slug", description = "Recupera los datos de una marca mediante su slug único.")
    public ResponseEntity<MarcaDto> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(marcaService.obtenerPorSlug(slug));
    }

    @PostMapping
    @Operation(summary = "Crear nueva marca", description = "Registra una nueva marca en el sistema.")
    public ResponseEntity<MarcaDto> crear(@Valid @RequestBody CrearMarcaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marcaService.crearMarca(dto));
    }
}
