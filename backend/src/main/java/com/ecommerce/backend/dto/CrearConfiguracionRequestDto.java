package com.ecommerce.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearConfiguracionRequestDto {

    @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
    private String nombre;

    @NotNull(message = "El presupuesto es obligatorio")
    @DecimalMin(value = "0.01", message = "El presupuesto debe ser mayor que cero")
    private BigDecimal presupuesto;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "La moneda debe usar un código ISO de 3 letras")
    @Builder.Default
    private String moneda = "PEN";
}
