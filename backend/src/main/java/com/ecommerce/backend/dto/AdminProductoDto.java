package com.ecommerce.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AdminProductoDto {
    private FichaProductoDto fichaTecnica;

    private UUID id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 220, message = "El nombre no puede exceder 220 caracteres")
    private String nombre;

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 80, message = "El SKU no puede exceder 80 caracteres")
    private String sku;

    private String slug;

    private String descripcion;

    private UUID categoriaId;

    private String categoriaNombre;

    private UUID marcaId;

    private String marcaNombre;

    private UUID tipoId;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio base no puede ser negativo")
    private BigDecimal precioBase;

    @DecimalMin(value = "0.00", message = "El costo de referencia no puede ser negativo")
    private BigDecimal costoReferencia;

    @Builder.Default
    private Integer stockInicial = 0;

    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    private Boolean vendible = true;

    private String estado;

    @Builder.Default
    private Boolean requiereSerie = false;

    @Builder.Default
    private Boolean requiereEnvio = true;

    private String imagenPrincipalUrl;

    @Builder.Default
    private List<ImagenProductoDto> imagenes = new ArrayList<>();
}
