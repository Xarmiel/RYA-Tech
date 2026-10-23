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
public class CategoriaDto {
    private UUID id;
    private UUID parentId;
    private String nombre;
    private String slug;
    private String acronimo;
    private Integer posicion;
}
