package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "necesidades_perfil", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NecesidadPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilAsesoria perfil;

    @Column(name = "codigo", length = 60)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 140)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "posicion", nullable = false)
    @Builder.Default
    private Integer posicion = 0;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
