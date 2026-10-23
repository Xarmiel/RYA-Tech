package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ActualizarPerfilRequestDto;
import com.ecommerce.backend.dto.PerfilUsuarioDto;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CuentaService {
    PerfilUsuarioDto obtenerOSincronizar(Jwt jwt);
    PerfilUsuarioDto actualizar(Jwt jwt, ActualizarPerfilRequestDto request);
}
