package com.ecommerce.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarritoDto {
    private UUID id;
    @Builder.Default
    private String moneda = "PEN";
    @Builder.Default
    private List<ItemCarritoDto> items = new ArrayList<>();
    @Builder.Default
    private Integer cantidadTotal = 0;
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
}
