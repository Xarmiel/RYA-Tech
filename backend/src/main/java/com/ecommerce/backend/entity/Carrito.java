package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "carritos", schema = "rya")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Carrito {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "session_key_hash", length = 100)
    private String sessionKeyHash;

    @Column(name = "estado", nullable = false, length = 25)
    @Builder.Default
    private String estado = "ABIERTO";

    @Column(name = "moneda", nullable = false, length = 3)
    @Builder.Default
    private String moneda = "PEN";

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
