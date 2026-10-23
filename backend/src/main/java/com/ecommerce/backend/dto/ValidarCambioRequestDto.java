package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCambioRequestDto {

    @Schema(description = "ID del procesador a validar")
    private UUID cpuId;

    @Schema(description = "ID del producto placa madre")
    private UUID placaId;

    @Schema(description = "ID de la revisión específica de la placa madre en rya.revisiones_placa")
    private UUID placaRevisionId;

    @Schema(description = "Versión de BIOS instalada en la placa para validación estricta")
    private String biosInstalada;

    @Schema(description = "Lista de IDs de otros productos ya agregados en la configuración")
    @Builder.Default
    private List<UUID> componentesActuales = new ArrayList<>();

    @Schema(description = "ID del producto memoria RAM seleccionado")
    private UUID ramId;

    @Schema(description = "ID del producto tarjeta gráfica seleccionado")
    private UUID gpuId;

    @Schema(description = "ID del producto fuente de poder seleccionado")
    private UUID fuenteId;

    @Schema(description = "ID del producto gabinete seleccionado")
    private UUID gabineteId;

    @Schema(description = "ID del producto sistema de refrigeración seleccionado")
    private UUID refrigeracionId;

    @Schema(description = "Componentes que el motor no puede retirar ni sustituir")
    @Builder.Default
    private List<UUID> componentesBloqueados = new ArrayList<>();
}
