package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movimientos_inventario", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "existencia_id", nullable = false)
    private Existencia existencia;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "referencia_tipo", length = 60)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "nota", columnDefinition = "text")
    private String nota;

    @Column(name = "registrado_at", nullable = false)
    private OffsetDateTime registradoAt;

    @PrePersist
    public void prePersist() {
        if (this.registradoAt == null) {
            this.registradoAt = OffsetDateTime.now();
        }
    }
}
