package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkComparadoDto {
    private String aplicacion;
    private String prueba;
    private String metrica;
    private String unidad;
    private Boolean mayorEsMejor;

    @Builder.Default
    private Map<UUID, BigDecimal> puntuaciones = new HashMap<>();
}
