package com.ecommerce.backend.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record TarifaEnvioRequest(
    @NotBlank @Size(max=160) String nombreServicio,
    @NotBlank @Pattern(regexp="DOMICILIO|RECOJO") String modalidad,
    @Size(max=100) String departamento, @Size(max=100) String provincia, @Size(max=100) String distrito,
    @NotNull @DecimalMin("0.00") @DecimalMax("999999.99") @Digits(integer=6,fraction=2) BigDecimal costo,
    @NotNull @Min(0) @Max(365) Integer plazoDias,
    @Size(max=300) String direccionRecojo, @Size(max=200) String horarioRecojo,
    @NotNull Boolean activo) {}
