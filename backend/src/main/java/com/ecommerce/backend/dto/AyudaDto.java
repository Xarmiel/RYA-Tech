package com.ecommerce.backend.dto;

import java.util.List;
import java.util.UUID;

public record AyudaDto(List<Pregunta> content, List<Categoria> categorias,
        int page, int size, long totalElements, int totalPages) {
    public record Pregunta(UUID id, String pregunta, String respuesta, UUID categoriaId, String categoria) {}
    public record Categoria(UUID id, String nombre) {}
}
