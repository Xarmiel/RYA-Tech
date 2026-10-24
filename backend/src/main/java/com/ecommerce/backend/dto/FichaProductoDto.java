package com.ecommerce.backend.dto;

import java.util.Map;
import java.util.List;

/** Commercial specifications only. This contract never certifies compatibility. */
public record FichaProductoDto(String tipo, String fuenteUrl, Map<String, Object> valores, String origen, List<String> avisos) {
    public FichaProductoDto(String tipo, String fuenteUrl, Map<String, Object> valores) {
        this(tipo, fuenteUrl, valores, "COMERCIAL", List.of());
    }
}
