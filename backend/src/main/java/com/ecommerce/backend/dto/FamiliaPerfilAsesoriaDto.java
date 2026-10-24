package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaPerfilAsesoriaDto {
    private UUID id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String icono;
    private Integer posicion;
    private Boolean activo;
}
