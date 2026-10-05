package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoUpdateDto(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    String nombre,

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0")
    BigDecimal precioBase,

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 500)
    String imagenUrl
) {}
