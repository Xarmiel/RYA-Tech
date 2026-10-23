package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuResumenDto {
    private UUID productoId;
    private String nombre;
    private String slug;
    private String marca;
    private String modelo;
    private String socket;
    private String familia;
    private String generacion;
    private Integer nucleos;
    private Integer hilos;
    private Integer frecuenciaBaseMhz;
    private Integer frecuenciaMaximaMhz;
    private BigDecimal potenciaBaseW;
    private BigDecimal potenciaMaximaW;
    private Boolean tieneGraficosIntegrados;
    private String modeloGraficos;
    private BigDecimal precio;
    private Integer stockDisponible;
    private String imagenUrl;
}
