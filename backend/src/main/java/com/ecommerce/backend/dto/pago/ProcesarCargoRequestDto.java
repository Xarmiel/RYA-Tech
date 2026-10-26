package com.ecommerce.backend.dto.pago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesarCargoRequestDto {
    @NotNull(message = "El ID del pedido es obligatorio")
    private UUID pedidoId;

    private String token; // Token de tarjeta generado por Brick

    private String paymentMethodId; // visa, master, yape, etc.

    private Integer installments; // Cuotas (default 1)

    private String issuerId;

    @NotBlank(message = "El email del pagador es obligatorio")
    private String email;
}

