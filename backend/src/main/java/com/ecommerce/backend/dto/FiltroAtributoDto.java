package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltroAtributoDto {
    private String codigo;
    private String nombre;
    private String tipoDato;
    private String unidad;

    @Builder.Default
    private List<String> valores = new ArrayList<>();
}
