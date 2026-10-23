package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "revisiones_placa", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionPlaca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(name = "modelo", nullable = false)
    private String modelo;

    @Column(name = "revision", nullable = false)
    private String revision;

    @Column(name = "socket", nullable = false)
    private String socket;

    @Column(name = "chipset", nullable = false)
    private String chipset;

    @Column(name = "fuente_id", nullable = false)
    private UUID fuenteId;

    @Column(name = "verificada_at", nullable = false)
    private OffsetDateTime verificadaAt;
}
