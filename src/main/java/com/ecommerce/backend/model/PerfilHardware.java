package com.ecommerce.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "perfiles_hardware", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PerfilHardware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false, unique = true)
    private Especialidad especialidad;

    @Column(name = "prio_cpu", nullable = false)
    private Short prioCpu;

    @Column(name = "prio_gpu", nullable = false)
    private Short prioGpu;

    @Column(name = "prio_disco", nullable = false)
    private Short prioDisco;

    @Column(name = "prio_ram", nullable = false)
    private Short prioRam;

    @Column(name = "prio_fuente", nullable = false)
    private Short prioFuente;

    @Column(name = "prio_placa", nullable = false)
    private Short prioPlaca;

    @Column(name = "prio_case", nullable = false)
    private Short prioCase;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requisitos_tecnicos", columnDefinition = "jsonb")
    private Map<String, Object> requisitosTecnicos;

    @Column(columnDefinition = "text")
    private String descripcion;
}
