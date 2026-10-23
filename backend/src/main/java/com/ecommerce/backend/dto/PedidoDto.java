package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoDto(UUID id, String codigo, String estado, String moneda,
        BigDecimal subtotal, BigDecimal descuento, BigDecimal envio, BigDecimal total,
        OffsetDateTime fecha, String metodoPago, List<Item> productos) {
    public record Item(UUID id, UUID productoId, String nombre, String sku, int cantidad,
            BigDecimal precioUnitario, BigDecimal descuentoUnitario, BigDecimal subtotal,
            String slug, String imagen) {}
}
