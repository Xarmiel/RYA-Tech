package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuComparacionResponseDto {

    @Schema(description = "Detalle completo de los procesadores comparados (máximo 3)")
    @Builder.Default
    private List<CpuDetalleDto> procesadores = new ArrayList<>();

    @Schema(description = "Diferencia de precio monetario respecto al primer procesador (referencia)")
    @Builder.Default
    private Map<UUID, BigDecimal> diferenciaPrecioReferencia = new HashMap<>();

    @Schema(description = "Matriz comparativa de benchmarks disponibles en común")
    @Builder.Default
    private List<BenchmarkComparadoDto> benchmarks = new ArrayList<>();
}
