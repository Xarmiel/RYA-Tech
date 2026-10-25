package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para recomendar una configuración de PC basada en perfil y presupuesto")
public class RecomendarPcRequestDto {

    @Schema(description = "Modalidad del configurador (presupuesto, manual, componente)", example = "presupuesto")
    private String mode;

    @NotNull(message = "El presupuesto es obligatorio")
    @DecimalMin(value = "500.00", message = "El presupuesto mínimo para armar una PC es S/ 500.00")
    @Schema(description = "Presupuesto máximo disponible en Soles (PEN)", example = "4500.00")
    private BigDecimal budget;

    @NotNull(message = "El perfil de asesoría es obligatorio")
    @Schema(description = "ID del perfil de asesoría seleccionado")
    private UUID profileId;

    @Schema(description = "ID opcional de necesidad específica")
    private UUID needId;

    @Schema(description = "ID opcional de resolución objetivo")
    private UUID resolutionId;

    @Schema(description = "ID opcional de producto ancla para armado híbrido")
    private UUID anchorProductId;
}
