package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfertaDto(UUID productoId, String nombre, String slug, String marca, String categoria,
        String imagen, int stockDisponible, BigDecimal precioAnterior, BigDecimal precioActual,
        BigDecimal descuentoPorcentaje, BigDecimal ahorro, String promocion,
        OffsetDateTime inicioAt, OffsetDateTime finAt) {}
