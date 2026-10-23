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
public class MarcaDto {
    private UUID id;
    private String nombre;
    private String slug;
    private String logoUrl;
    private String descripcion;
}
