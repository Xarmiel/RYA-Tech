package com.ecommerce.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CheckoutValidacionDto {
    boolean valido;
    boolean requiereAutenticacion;
    String moneda;
    BigDecimal subtotal;
    BigDecimal envio;
    BigDecimal total;
    String metodoPago;
    String entrega;
    List<String> mensajes;
    CarritoDto carrito;
}
