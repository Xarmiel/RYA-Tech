package com.ecommerce.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tarifas_envio", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
public class TarifaEnvio {
    @Id
    private UUID id;
    @Column(name = "transportista_id")
    private UUID transportistaId;
    @Column(nullable = false)
    private Boolean activo = true;
    @Column(nullable = false)
    private String modalidad = "DOMICILIO";
    @Column(name = "nombre_servicio")
    private String nombreServicio;
    @Column(name = "direccion_recojo")
    private String direccionRecojo;
    @Column(name = "horario_recojo")
    private String horarioRecojo;
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
