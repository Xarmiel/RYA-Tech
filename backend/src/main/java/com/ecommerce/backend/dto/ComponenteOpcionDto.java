package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Componente recomendado con datos comerciales y técnicos para el PC Builder")
public class ComponenteOpcionDto {

    @Schema(description = "ID único del producto")
    private UUID id;

    @Schema(description = "Código del tipo de componente (ej: CPU, PLACA, RAM, GPU, etc.)", example = "CPU")
    private String componentTypeCode;

    @Schema(description = "Nombre comercial del producto")
    private String name;

    @Schema(description = "Fabricante / Marca del producto")
    private String brand;

    @Schema(description = "Slug amigable para URLs")
    private String slug;

    @Schema(description = "Resumen o SKU del producto")
    private String summary;

    @Schema(description = "Precio unitario en Soles")
    private BigDecimal price;

    @Schema(description = "Moneda del precio", example = "PEN")
    @Builder.Default
    private String currency = "PEN";

    @Schema(description = "Stock físico disponible")
    private Integer stock;

    @Schema(description = "URL de imagen principal")
    private String imageUrl;

    @Schema(description = "URLs de imágenes disponibles")
    private List<String> imageUrls;

    @Schema(description = "Nivel de compatibilidad", example = "VERIFIED")
    @Builder.Default
    private String compatibility = "VERIFIED";
}
