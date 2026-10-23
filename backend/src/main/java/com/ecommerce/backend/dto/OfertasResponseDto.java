package com.ecommerce.backend.dto;

import java.util.List;

public record OfertasResponseDto(List<OfertaDto> content, int page, int size, long totalElements,
        int totalPages, List<Filtro> categorias, List<Filtro> marcas) {
    public record Filtro(String slug, String nombre) {}
}
