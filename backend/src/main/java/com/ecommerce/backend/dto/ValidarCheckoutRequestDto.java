package com.ecommerce.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ValidarCheckoutRequestDto {
    @Valid @NotNull private Cliente cliente;
    @Valid @NotNull private Entrega entrega;
    @NotNull private UUID metodoPagoId;

    @Data
    public static class Cliente {
        @NotBlank @Pattern(regexp = "DNI|RUC|CE") private String tipoDocumento;
        @NotBlank @Size(min = 6, max = 12) private String numeroDocumento;
        @NotBlank @Size(min = 3, max = 140) private String nombre;
        @NotBlank @Email @Size(max = 254) private String email;
        @NotBlank @Pattern(regexp = "[0-9]{9}") private String telefono;
    }

    @Data
    public static class Entrega {
        @NotNull private UUID tarifaEnvioId;
        @Size(max = 300) private String direccion;
        @Size(max = 100) private String departamento;
        @Size(max = 100) private String provincia;
        @Size(max = 100) private String distrito;
        @Size(max = 300) private String referencia;
    }
}
