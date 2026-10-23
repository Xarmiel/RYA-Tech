package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "compatibilidad_cpu_placa", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompatibilidadCpuPlaca {

    @EmbeddedId
    private CompatibilidadCpuPlacaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id", insertable = false, updatable = false)
    private EspecificacionCpu cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placa_revision_id", insertable = false, updatable = false)
    private RevisionPlaca revisionPlaca;

    @Column(name = "soporte", nullable = false)
    private String soporte;

    @Column(name = "bios_minima_id")
    private UUID biosMinimaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bios_minima_id", insertable = false, updatable = false)
    private VersionBios biosMinima;

    @Column(name = "condiciones", columnDefinition = "text")
    private String condiciones;

    @Column(name = "fuente_id", nullable = false)
    private UUID fuenteId;

    @Column(name = "verificada_at", nullable = false)
    private OffsetDateTime verificadaAt;
}
