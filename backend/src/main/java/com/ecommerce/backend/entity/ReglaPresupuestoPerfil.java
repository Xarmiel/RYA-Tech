package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reglas_presupuesto_perfil", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaPresupuestoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilAsesoria perfil;

    @Column(name = "tipo_componente_codigo", nullable = false, length = 50)
    private String tipoComponenteCodigo;

    @Column(name = "prioridad")
    private Integer prioridad;

    @Column(name = "porcentaje_min", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeMin = BigDecimal.ZERO;

    @Column(name = "porcentaje_max", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeMax = BigDecimal.ZERO;

    @Column(name = "obligatorio", nullable = false)
    @Builder.Default
    private Boolean obligatorio = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
