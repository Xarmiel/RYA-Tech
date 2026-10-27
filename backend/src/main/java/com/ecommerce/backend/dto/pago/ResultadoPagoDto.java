package com.ecommerce.backend.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoPagoDto {
    private boolean exitoso;
    private UUID pedidoId;
    private String numeroPedido;
    private String estadoPedido;
    private String mpPaymentId;
    private String mpOrderId;
    private String estadoPago;
    private boolean pendiente;
    private String instruccionesUrl;
    private String referenciaPago;
    private String mensaje;
    private BigDecimal monto;
    private String moneda;
}

