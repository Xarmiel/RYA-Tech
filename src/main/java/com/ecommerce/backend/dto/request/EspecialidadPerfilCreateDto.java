package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.*;

public record EspecialidadPerfilCreateDto(
    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(max = 100)
    String nombre,

    @NotBlank(message = "El campo de la especialidad es obligatorio")
    @Size(max = 100)
    String campo,

    @NotNull(message = "La prioridad de CPU es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioCpu,

    @NotNull(message = "La prioridad de GPU es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioGpu,

    @NotNull(message = "La prioridad de Disco es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioDisco,

    @NotNull(message = "La prioridad de RAM es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioRam,

    @NotNull(message = "La prioridad de Fuente es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioFuente,

    @NotNull(message = "La prioridad de Placa es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioPlaca,

    @NotNull(message = "La prioridad de Case es obligatoria")
    @Min(value = 1, message = "La prioridad debe estar entre 1 y 7")
    @Max(value = 7, message = "La prioridad debe estar entre 1 y 7")
    Short prioCase,

    @NotBlank(message = "La descripción de la especialidad es obligatoria")
    String descripcion
) {}
