package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ActualizarRolRequestDto;
import com.ecommerce.backend.dto.AdminUsuarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface AdminUsuarioService {
    PaginatedResponseDto<AdminUsuarioDto> listarUsuarios(String busqueda, String rol, int page, int size);
    AdminUsuarioDto cambiarRol(UUID usuarioId, ActualizarRolRequestDto request, Jwt jwtActor);
}
