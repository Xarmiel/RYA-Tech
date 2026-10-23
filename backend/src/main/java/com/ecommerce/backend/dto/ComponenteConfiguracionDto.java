package com.ecommerce.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponenteConfiguracionDto {
    private UUID id;
    private UUID tipoComponenteId;
    private String tipoCodigo;
    private String tipoNombre;
    private UUID productoId;
    private String sku;
    private String productoNombre;
    private Integer cantidad;
    private Boolean bloqueado;
    private BigDecimal precioSnapshot;
    private BigDecimal subtotal;
    private Integer stockDisponible;
}
