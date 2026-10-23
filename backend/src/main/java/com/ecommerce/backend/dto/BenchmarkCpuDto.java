package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkCpuDto {
    private UUID id;
    private String aplicacion;
    private String versionAplicacion;
    private String prueba;
    private String metrica;
    private String unidad;
    private BigDecimal valor;
    private Boolean mayorEsMejor;
    private LocalDate medidoEl;
}
