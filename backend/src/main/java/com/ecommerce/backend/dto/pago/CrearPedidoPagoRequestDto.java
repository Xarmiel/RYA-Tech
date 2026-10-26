package com.ecommerce.backend.dto.pago;

import com.ecommerce.backend.dto.ValidarCheckoutRequestDto;
import jakarta.validation.Valid;
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
public class CrearPedidoPagoRequestDto {
    @NotNull(message = "Los datos del cliente son requeridos")
    @Valid
    private ValidarCheckoutRequestDto.Cliente cliente;

    @NotNull(message = "Los datos de entrega son requeridos")
    @Valid
    private ValidarCheckoutRequestDto.Entrega entrega;

    @NotNull(message = "El método de pago es requerido")
    private UUID metodoPagoId;
}
