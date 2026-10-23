package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCambioResponseDto {

    @Schema(description = "Indica si la combinación es plenamente compatible y ejecutable")
    private Boolean esCompatible;

    @Schema(description = "Estado de compatibilidad: VERIFICADA, CONDICIONAL, INCOMPATIBLE, POR_VERIFICAR")
    private String estadoCompatibilidad;

    @Schema(description = "Mensajes informativos o causas de incompatibilidad")
    @Builder.Default
    private List<String> mensajes = new ArrayList<>();

    @Schema(description = "Indica si se requiere actualizar la BIOS para soportar este CPU")
    private Boolean requiereActualizacionBios;

    @Schema(description = "Versión mínima de BIOS requerida si aplica")
    private String biosMinimaRequerida;

    @Schema(description = "Condiciones o notas técnicas adicionales documentadas por el fabricante")
    private String condiciones;

    @Schema(description = "Detalles técnicos adicionales del chequeo")
    @Builder.Default
    private Map<String, Object> detallesTecnicos = new HashMap<>();
}
