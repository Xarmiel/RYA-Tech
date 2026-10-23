package com.ecommerce.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteStockRequestDto {

    private UUID existenciaId;
    private UUID varianteId;

    @NotNull
    private UUID operacionId;

    @NotNull
    @Min(-1)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = InventarioIntegerDeserializer.class)
    private Integer versionEsperada;

    @NotNull(message = "El producto es obligatorio")
    private UUID productoId;

    @NotNull(message = "El almacén es obligatorio")
    private UUID almacenId;

    @NotBlank(message = "El tipo de movimiento es obligatorio (ENTRADA, SALIDA o AJUSTE)")
    @Pattern(regexp = "ENTRADA|SALIDA|AJUSTE", message = "Tipo debe ser ENTRADA, SALIDA o AJUSTE")
    private String tipo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad debe ser mayor o igual a 0")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = InventarioIntegerDeserializer.class)
    private Integer cantidad;

    @NotBlank(message = "El motivo o justificación del ajuste es obligatorio para la trazabilidad")
    @Size(max = 2000)
    private String nota;
}
