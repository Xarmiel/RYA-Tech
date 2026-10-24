package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearCategoriaDto {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    @Size(max = 150, message = "El slug no puede exceder 150 caracteres")
    private String slug;

    @Size(max = 20, message = "El acrónimo no puede exceder 20 caracteres")
    private String acronimo;

    private UUID parentId;

    private Integer posicion;
}
