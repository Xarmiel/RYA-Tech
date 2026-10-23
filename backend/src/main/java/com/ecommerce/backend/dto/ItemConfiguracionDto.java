package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemConfiguracionDto {

    @NotNull(message = "El ID del producto es obligatorio")
    @Schema(description = "ID del producto existente en la base de datos")
    private UUID productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima debe ser al menos 1")
    @Builder.Default
    @Schema(description = "Cantidad de unidades del producto", example = "1")
    private Integer cantidad = 1;
}
