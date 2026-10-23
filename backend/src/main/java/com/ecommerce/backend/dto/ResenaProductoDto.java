package com.ecommerce.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class ResenaProductoDto {
    UUID id;
    int calificacion;
    String comentario;
    String autor;
    OffsetDateTime fecha;
}
