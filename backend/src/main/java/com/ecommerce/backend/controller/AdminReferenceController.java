package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.AdminReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/referencias")
@RequiredArgsConstructor
public class AdminReferenceController {
    private final AdminReferenceService service;

    @GetMapping
    public AdminReferenceService.Listado listar(@RequestParam(defaultValue = "false") boolean incluirEliminadas) {
        return service.listar(incluirEliminadas);
    }

    @DeleteMapping("/marcas/{id}")
    public ResponseEntity<Void> eliminarMarca(@PathVariable UUID id) {
        service.eliminarMarca(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable UUID id) {
        service.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marcas/{id}/restaurar")
    public ResponseEntity<Void> restaurarMarca(@PathVariable UUID id) {
        service.restaurarMarca(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/categorias/{id}/restaurar")
    public ResponseEntity<Void> restaurarCategoria(@PathVariable UUID id) {
        service.restaurarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
