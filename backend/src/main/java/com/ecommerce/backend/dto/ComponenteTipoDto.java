package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponenteTipoDto {
    private UUID id;
    private String codigo;
    private String nombre;
    private Boolean obligatorio;

    @Builder.Default
    private List<ProductoResumenDto> productos = new ArrayList<>();
}
