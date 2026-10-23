package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtributoValorDto {
    private String codigo;
    private String nombre;
    private String tipoDato;
    private String unidad;
    private String valor;
}
