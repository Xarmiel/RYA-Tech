package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "benchmarks_cpu", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkCpu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cpu_id", nullable = false)
    private UUID cpuId;

    @Column(name = "aplicacion", nullable = false)
    private String aplicacion;

    @Column(name = "version_aplicacion", nullable = false)
    private String versionAplicacion;

    @Column(name = "prueba", nullable = false)
    private String prueba;

    @Column(name = "metrica", nullable = false)
    private String metrica;

    @Column(name = "unidad", nullable = false)
    private String unidad;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "mayor_es_mejor", nullable = false)
    private Boolean mayorEsMejor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuracion_prueba", columnDefinition = "jsonb")
    private String configuracionPrueba;

    @Column(name = "medido_el", nullable = false)
    private LocalDate medidoEl;

    @Column(name = "fuente_id", nullable = false)
    private UUID fuenteId;

    @Column(name = "verificada_at", nullable = false)
    private OffsetDateTime verificadaAt;
}
