package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCreateDto(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    String nombre,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 150)
    String email,

    @Size(max = 25)
    String telefono,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,

    String rol
) {}