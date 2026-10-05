package com.ecommerce.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especialidades", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String campo;
}
