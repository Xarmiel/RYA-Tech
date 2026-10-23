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
public class FiltrosDisponiblesDto {
    @Builder.Default
    private List<FiltroOpcionDto> categorias = new ArrayList<>();

    @Builder.Default
    private List<FiltroOpcionDto> marcas = new ArrayList<>();

    private FiltroRangoPrecioDto rangoPrecio;

    @Builder.Default
    private List<FiltroAtributoDto> atributos = new ArrayList<>();
}
