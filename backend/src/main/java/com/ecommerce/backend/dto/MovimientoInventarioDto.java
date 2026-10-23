package com.ecommerce.backend.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioDto {
    private String actor;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private Integer delta;
    private UUID id;
    private UUID existenciaId;
    private String tipo;
    private Integer cantidad;
    private String referenciaTipo;
    private UUID referenciaId;
    private String nota;
    private OffsetDateTime registradoAt;
}
