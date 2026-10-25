package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "compatibilidad_cpu_chipset", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompatibilidadCpuChipset {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "socket", nullable = false)
    private String socket;

    @Column(name = "generacion", nullable = false)
    private String generacion;

    @Column(name = "chipset", nullable = false)
    private String chipset;

    @Column(name = "requiere_actualizacion", nullable = false)
    private Boolean requiereActualizacion;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
