package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ActualizarRolRequestDto;
import com.ecommerce.backend.dto.AdminUsuarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.service.AdminUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequiredArgsConstructor
@Tag(name = "Admin - Usuarios", description = "Gestión de cuentas y roles de usuarios y administradores")
public class AdminUsuarioController {

    private final AdminUsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Listar cuentas de usuarios con filtros y paginación")
    public ResponseEntity<PaginatedResponseDto<AdminUsuarioDto>> listarUsuarios(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(busqueda, rol, page, size));
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar rol de un usuario (admin o user)")
    public ResponseEntity<AdminUsuarioDto> cambiarRol(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarRolRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request, jwt));
    }
}
