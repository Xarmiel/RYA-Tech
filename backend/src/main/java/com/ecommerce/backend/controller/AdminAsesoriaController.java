package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.FamiliaPerfilAsesoriaDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.PerfilAsesoriaDto;
import com.ecommerce.backend.service.AdminAsesoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/asesorias")
@RequiredArgsConstructor
@Tag(name = "Admin - Asesorías y Perfiles", description = "Gestión de perfiles de uso, familias y reglas de recomendación para asesoria de PC")
public class AdminAsesoriaController {

    private final AdminAsesoriaService adminAsesoriaService;

    @GetMapping
    @Operation(summary = "Listar perfiles de asesoría paginados con filtros")
    public ResponseEntity<PaginatedResponseDto<PerfilAsesoriaDto>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) UUID familiaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminAsesoriaService.listarPerfiles(busqueda, activo, familiaId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle completo de un perfil de asesoría")
    public ResponseEntity<PerfilAsesoriaDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAsesoriaService.obtenerPerfilPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo perfil de asesoría con sus reglas")
    public ResponseEntity<PerfilAsesoriaDto> crear(@Valid @RequestBody PerfilAsesoriaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAsesoriaService.crearPerfil(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar perfil de asesoría y sus reglas")
    public ResponseEntity<PerfilAsesoriaDto> actualizar(@PathVariable UUID id, @Valid @RequestBody PerfilAsesoriaDto dto) {
        return ResponseEntity.ok(adminAsesoriaService.actualizarPerfil(id, dto));
    }

    @PatchMapping("/{id}/publicar")
    @Operation(summary = "Activar / publicar perfil de asesoría")
    public ResponseEntity<PerfilAsesoriaDto> publicar(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAsesoriaService.cambiarEstado(id, true));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar perfil de asesoría")
    public ResponseEntity<PerfilAsesoriaDto> desactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAsesoriaService.cambiarEstado(id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (borrado lógico) un perfil de asesoría")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable UUID id) {
        adminAsesoriaService.eliminarPerfil(id);
        return ResponseEntity.ok(Map.of("message", "Perfil eliminado correctamente"));
    }

    @GetMapping("/familias")
    @Operation(summary = "Listar familias de perfiles de asesoría")
    public ResponseEntity<List<FamiliaPerfilAsesoriaDto>> listarFamilias() {
        return ResponseEntity.ok(adminAsesoriaService.listarFamilias());
    }

    @PostMapping("/familias")
    @Operation(summary = "Crear una nueva familia de perfiles")
    public ResponseEntity<FamiliaPerfilAsesoriaDto> crearFamilia(@Valid @RequestBody FamiliaPerfilAsesoriaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAsesoriaService.crearFamilia(dto));
    }
}
