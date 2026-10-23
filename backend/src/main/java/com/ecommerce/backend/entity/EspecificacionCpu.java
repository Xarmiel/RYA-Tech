package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "especificaciones_cpu", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecificacionCpu {

    @Id
    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(name = "modelo", nullable = false)
    private String modelo;

    @Column(name = "socket", nullable = false)
    private String socket;

    @Column(name = "familia", nullable = false)
    private String familia;

    @Column(name = "generacion", nullable = false)
    private String generacion;

    @Column(name = "nucleos", nullable = false)
    private Integer nucleos;

    @Column(name = "hilos", nullable = false)
    private Integer hilos;

    @Column(name = "frecuencia_base_mhz")
    private Integer frecuenciaBaseMhz;

    @Column(name = "frecuencia_maxima_mhz")
    private Integer frecuenciaMaximaMhz;

    @Column(name = "cache_l1_kb")
    private BigDecimal cacheL1Kb;

    @Column(name = "cache_l2_kb")
    private BigDecimal cacheL2Kb;

    @Column(name = "cache_l3_kb")
    private BigDecimal cacheL3Kb;

    @Column(name = "potencia_base_w", nullable = false)
    private BigDecimal potenciaBaseW;

    @Column(name = "potencia_maxima_w")
    private BigDecimal potenciaMaximaW;

    @Column(name = "tiene_graficos_integrados", nullable = false)
    private Boolean tieneGraficosIntegrados;

    @Column(name = "modelo_graficos")
    private String modeloGraficos;

    @Column(name = "fuente_id", nullable = false)
    private UUID fuenteId;

    @Column(name = "verificada_at", nullable = false)
    private OffsetDateTime verificadaAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
