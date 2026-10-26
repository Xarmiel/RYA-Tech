package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarRolRequestDto {
    @NotBlank(message = "El rol no puede estar vacío")
    @Pattern(regexp = "^(?i)(admin|user)$", message = "El rol debe ser 'admin' o 'user'")
    private String rol;
}
