package com.ecommerce.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponseDto(
    UUID id,
    String nombre,
    String email,
    String telefono,
    String rol,
    OffsetDateTime fechaRegistro
) {}