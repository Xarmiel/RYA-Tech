package com.ecommerce.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CheckoutOpcionesDto {
    List<MetodoPagoDto> metodosPago;
    List<TarifaEnvioDto> tarifasEnvio;

    @Value @Builder
    public static class MetodoPagoDto { UUID id; String codigo; String nombre; }

    @Value @Builder
    public static class TarifaEnvioDto {
        UUID id;
        String transportista;
        String departamento;
        String provincia;
        String distrito;
        BigDecimal costo;
        Integer plazoDias;
    }
}
