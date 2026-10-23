package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios", schema = "rya")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 140)
    private String nombre;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "estado", nullable = false, length = 24)
    @Builder.Default
    private String estado = "ACTIVO";

    @Column(name = "email_verificado_at")
    private OffsetDateTime emailVerificadoAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
