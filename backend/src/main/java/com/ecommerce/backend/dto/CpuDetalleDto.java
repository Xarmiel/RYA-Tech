package com.ecommerce.backend.dto;

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
public class CpuDetalleDto {
    private UUID productoId;
    private String nombre;
    private String slug;
    private String sku;
    private String descripcion;
    private String marca;
    private String modelo;
    private String socket;
    private String familia;
    private String generacion;
    private Integer nucleos;
    private Integer hilos;
    private Integer frecuenciaBaseMhz;
    private Integer frecuenciaMaximaMhz;
    private BigDecimal cacheL1Kb;
    private BigDecimal cacheL2Kb;
    private BigDecimal cacheL3Kb;
    private BigDecimal potenciaBaseW;
    private BigDecimal potenciaMaximaW;
    private Boolean tieneGraficosIntegrados;
    private String modeloGraficos;
    private BigDecimal precio;
    private Integer stockDisponible;

    @Builder.Default
    private List<ImagenProductoDto> imagenes = new ArrayList<>();

    @Builder.Default
    private List<BenchmarkCpuDto> benchmarks = new ArrayList<>();
}
