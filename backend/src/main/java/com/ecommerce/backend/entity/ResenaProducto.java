package com.ecommerce.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resenas_producto", schema = "rya")
@Getter
@NoArgsConstructor
public class ResenaProducto {
    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "calificacion", nullable = false)
    private Short calificacion;

    @Column(name = "comentario")
    private String comentario;

    @Column(name = "publicada", nullable = false)
    private Boolean publicada;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
