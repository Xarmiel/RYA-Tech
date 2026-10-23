package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CategoriaDto;
import com.ecommerce.backend.dto.CrearCategoriaDto;
import com.ecommerce.backend.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Operaciones de consulta y gestión sobre categorías")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "Listar categorías activas", description = "Devuelve todas las categorías de productos activas y no eliminadas.")
    public ResponseEntity<List<CategoriaDto>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.listarActivas());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Obtener una categoría por slug", description = "Recupera la información de una categoría mediante su slug único.")
    public ResponseEntity<CategoriaDto> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoriaService.obtenerPorSlug(slug));
    }

    @PostMapping
    @Operation(summary = "Crear nueva categoría", description = "Registra una nueva categoría en el sistema.")
    public ResponseEntity<CategoriaDto> crear(@Valid @RequestBody CrearCategoriaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crearCategoria(dto));
    }
}
