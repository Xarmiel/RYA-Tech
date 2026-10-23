package com.ecommerce.backend.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioItemDto {
    private UUID varianteId;
    private Integer version;
    private UUID existenciaId;
    private UUID productoId;
    private String productoNombre;
    private String productoSku;
    private String productoSlug;
    private String marca;
    private String categoria;
    private String imagenPrincipal;
    private UUID almacenId;
    private String almacenNombre;
    private String almacenCodigo;
    private Integer cantidad;
    private Integer reservado;
    private Integer disponible;
    private String estadoAlerta;
}
