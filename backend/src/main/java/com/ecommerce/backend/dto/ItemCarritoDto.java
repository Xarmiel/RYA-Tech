package com.ecommerce.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ItemCarritoDto {
    private UUID id;
    private UUID productoId;
    private String sku;
    private String nombre;
    private String slug;
    private String imagen;
    private Integer cantidad;
    private Integer stockDisponible;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Boolean precioActualizado;
}
