package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "configuraciones_pc", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionPc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "nombre", length = 160)
    private String nombre;

    @Column(name = "presupuesto", nullable = false, precision = 14, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "moneda", nullable = false, length = 3)
    @Builder.Default
    private String moneda = "PEN";

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "BORRADOR";

    @Column(name = "total_estimado", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalEstimado = BigDecimal.ZERO;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
