package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "versiones_bios", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionBios {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "placa_revision_id", nullable = false)
    private UUID placaRevisionId;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "publicada_el")
    private LocalDate publicadaEl;

    @Column(name = "fuente_id", nullable = false)
    private UUID fuenteId;
}
