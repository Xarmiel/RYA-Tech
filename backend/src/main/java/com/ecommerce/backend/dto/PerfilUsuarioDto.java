package com.ecommerce.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PerfilUsuarioDto {
    UUID id;
    String nombre;
    String email;
    String telefono;
    String estado;
    boolean emailVerificado;
}
