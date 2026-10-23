package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcularConfiguracionResponseDto {

    @Schema(description = "Detalle de cálculo por cada componente")
    @Builder.Default
    private List<ItemCalculadoDto> items = new ArrayList<>();

    @Schema(description = "Costo total de la configuración en BigDecimal")
    private BigDecimal precioTotal;

    @Schema(description = "Consumo eléctrico total estimado en Watts")
    private BigDecimal consumoEstimadoW;

    @Schema(description = "Potencia sugerida de fuente de poder en Watts con margen seguro de operación")
    private Integer fuenteRecomendadaW;

    @Schema(description = "Indica si todos los componentes tienen stock disponible inmediato")
    private Boolean todosDisponibles;

    @Schema(description = "Indica si faltan datos de consumo (TDP/potencia) en algún componente en la BD")
    private Boolean datosConsumoIncompletos;

    @Schema(description = "Observaciones o avisos respecto a compatibilidad, potencia y disponibilidad")
    @Builder.Default
    private List<String> notas = new ArrayList<>();
}
