package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarPerfilRequestDto {
    @NotBlank @Size(min = 3, max = 140)
    private String nombre;

    @Pattern(regexp = "^$|[0-9+() -]{7,30}$", message = "El teléfono no tiene un formato válido")
    private String telefono;
}
