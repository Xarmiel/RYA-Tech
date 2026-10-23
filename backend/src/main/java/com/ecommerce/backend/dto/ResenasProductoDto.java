package com.ecommerce.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class ResenasProductoDto {
    BigDecimal promedio;
    int total;
    List<ResenaProductoDto> resenas;
}
