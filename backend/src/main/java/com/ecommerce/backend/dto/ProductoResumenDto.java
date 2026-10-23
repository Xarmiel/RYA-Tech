package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResumenDto {
    private UUID id;
    private String sku;
    private String nombre;
    private String slug;
    private BigDecimal precioBase;
    private Boolean activo;
    private Boolean vendible;
    private String marca;
    private String marcaSlug;
    private String categoria;
    private String categoriaSlug;
    private String imagenPrincipal;
    private Integer stockDisponible;
}
