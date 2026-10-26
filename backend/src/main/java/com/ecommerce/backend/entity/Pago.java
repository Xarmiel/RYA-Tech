package com.ecommerce.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos", schema = "rya")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String pasarela = "MERCADO_PAGO";

    @Column(name = "mp_payment_id", length = 64)
    private String mpPaymentId;

    @Column(name = "payment_method_id", length = 64)
    private String paymentMethodId;

    @Column(name = "payment_type_id", length = 30)
    private String paymentTypeId;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;


    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moneda = "PEN";

    @Column(nullable = false, length = 30)
    private String estado; // approved, in_process, rejected

    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    @Column(name = "codigo_respuesta", length = 100)
    private String codigoRespuesta;


    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
