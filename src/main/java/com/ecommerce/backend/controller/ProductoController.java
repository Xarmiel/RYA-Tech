package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.ProductoCreateDto;
import com.ecommerce.backend.dto.response.ProductoResponseDto;
import com.ecommerce.backend.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    public ResponseEntity<ProductoResponseDto> crearProducto(@Valid @RequestBody ProductoCreateDto dto) {
        ProductoResponseDto nuevoProducto = productoService.crearProducto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponseDto>> listarProductos(
            @RequestParam(required = false) Integer categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(productoService.listarPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(productoService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ProductoResponseDto> obtenerPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(productoService.obtenerPorNombre(nombre));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDto> actualizarProducto(
            @PathVariable UUID id,
            @Valid @RequestBody com.ecommerce.backend.dto.request.ProductoUpdateDto dto) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarProducto(@PathVariable UUID id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/nombre")
    public ResponseEntity<Void> desactivarPorNombre(@RequestParam String nombre) {
        productoService.desactivarPorNombre(nombre);
        return ResponseEntity.noContent().build();
    }
}