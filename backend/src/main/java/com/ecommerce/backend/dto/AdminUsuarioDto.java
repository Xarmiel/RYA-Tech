package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUsuarioDto {
    private UUID id;
    private String nombre;
    private String email;
    private String telefono;
    private String rol;
    private String estado;
    private long totalPedidos;
    private OffsetDateTime ultimaCompra;
    private OffsetDateTime createdAt;
}
