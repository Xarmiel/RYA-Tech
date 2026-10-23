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
public class ImagenProductoDto {
    private UUID id;
    private String url;
    private String textoAlternativo;
    private Integer posicion;
    private Boolean principal;
}
