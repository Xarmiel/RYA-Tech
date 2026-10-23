package com.ecommerce.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tarifas_envio", schema = "rya")
@Getter
@NoArgsConstructor
public class TarifaEnvio {
    @Id
    private UUID id;
    @Column(name = "transportista_id", nullable = false)
    private UUID transportistaId;
    private String departamento;
    private String provincia;
    private String distrito;
    @Column(nullable = false)
    private BigDecimal costo;
    @Column(name = "plazo_dias")
    private Integer plazoDias;
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
