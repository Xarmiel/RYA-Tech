package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductoDetalleDto {
    private FichaProductoDto fichaTecnica;
    private UUID id;
    private String sku;
    private String nombre;
    private String slug;
    private String descripcion;
    private BigDecimal precioBase;
    private BigDecimal costoReferencia;
    private Boolean activo;
    private Boolean vendible;
    private Boolean requiereSerie;
    private Boolean requiereEnvio;
    private String marca;
    private String marcaSlug;
    private String categoria;
    private String categoriaSlug;
    private String tipo;
    private Integer stockDisponible;

    @Builder.Default
    private List<ImagenProductoDto> imagenes = new ArrayList<>();

    @Builder.Default
    private List<AtributoValorDto> atributos = new ArrayList<>();
}
