package com.ecommerce.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AplicarCambioConfiguracionRequestDto {

    @NotNull(message = "La configuración es obligatoria")
    private UUID configuracionId;

    @NotBlank(message = "El tipo de componente es obligatorio")
    private String tipoComponente;

    @NotNull(message = "El producto es obligatorio")
    private UUID productoId;

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Builder.Default
    private Integer cantidad = 1;

    @Builder.Default
    private Boolean bloqueado = false;

    private String biosInstalada;
}
