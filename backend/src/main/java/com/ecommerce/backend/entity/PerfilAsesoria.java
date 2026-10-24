package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "perfiles_asesoria", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilAsesoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = false)
    private FamiliaPerfilAsesoria familia;

    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 140)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "prioridad_cpu", nullable = false)
    @Builder.Default
    private Integer prioridadCpu = 5;

    @Column(name = "prioridad_gpu", nullable = false)
    @Builder.Default
    private Integer prioridadGpu = 5;

    @Column(name = "ram_minima_gb", nullable = false)
    @Builder.Default
    private Integer ramMinimaGb = 8;

    @Column(name = "requiere_gpu_dedicada", nullable = false)
    @Builder.Default
    private Boolean requiereGpuDedicada = false;

    @Column(name = "presupuesto_sugerido_min", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal presupuestoSugeridoMin = BigDecimal.ZERO;

    @Column(name = "presupuesto_sugerido_max", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal presupuestoSugeridoMax = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NecesidadPerfil> necesidades = new ArrayList<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReglaPresupuestoPerfil> reglasPresupuesto = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
