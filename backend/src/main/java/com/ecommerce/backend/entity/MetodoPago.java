package com.ecommerce.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "metodos_pago", schema = "rya")
@Getter
@NoArgsConstructor
public class MetodoPago {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String codigo;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private Boolean activo;
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
