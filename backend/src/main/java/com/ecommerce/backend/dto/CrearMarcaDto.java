package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearMarcaDto {

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 130, message = "El nombre no puede exceder 130 caracteres")
    private String nombre;

    private String slug;

    private String logoUrl;

    private String descripcion;
}
