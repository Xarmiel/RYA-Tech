package com.ecommerce.backend.dto.pago;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarPagoRequestDto {
    @NotBlank(message = "El método de pago es requerido")
    private String metodoPagoId;
    
    // El backend revalida todo el carrito y cliente usando la session/usuario
}
