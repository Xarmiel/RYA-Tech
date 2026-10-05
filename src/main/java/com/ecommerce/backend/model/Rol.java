package com.ecommerce.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;

    public Rol(String nombre) {
        this.nombre = nombre;
    }
}
