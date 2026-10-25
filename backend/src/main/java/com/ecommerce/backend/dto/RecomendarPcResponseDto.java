package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de recomendación de PC con componentes compatibles y métricas")
public class RecomendarPcResponseDto {

    @Schema(description = "Lista de componentes seleccionados para la configuración")
    private List<ComponenteOpcionDto> components;

    @Schema(description = "Costo total de la configuración recomendada")
    private BigDecimal total;

    @Schema(description = "Moneda del ensamble", example = "PEN")
    @Builder.Default
    private String currency = "PEN";

    @Schema(description = "Consumo eléctrico total estimado en Watts", example = "350")
    private Integer powerDrawWatts;

    @Schema(description = "Potencia sugerida de la fuente de poder en Watts", example = "650")
    private Integer recommendedPsuWatts;

    @Schema(description = "Indica si la configuración es 100% compatible", example = "true")
    private Boolean compatible;

    @Schema(description = "Notas, advertencias o aclaraciones de la configuración")
    private List<String> warnings;

    @Schema(description = "Presupuesto ingresado por el usuario", example = "4500.00")
    private BigDecimal budget;
}
