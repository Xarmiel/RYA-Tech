package com.ecommerce.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AplicarCambioConfiguracionResponseDto {
    private ConfiguracionPcDto configuracion;
    private ValidarCambioResponseDto compatibilidad;
}
