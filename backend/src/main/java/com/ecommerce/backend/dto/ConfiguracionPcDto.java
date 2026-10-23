package com.ecommerce.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionPcDto {
    private UUID id;
    private String nombre;
    private BigDecimal presupuesto;
    private String moneda;
    private String estado;
    private BigDecimal totalEstimado;
    private BigDecimal presupuestoRestante;
    @Builder.Default
    private List<ComponenteConfiguracionDto> componentes = new ArrayList<>();
}
