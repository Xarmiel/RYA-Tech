package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcularConfiguracionRequestDto {

    @NotEmpty(message = "Debe proporcionar al menos un componente para calcular")
    @Schema(description = "Lista de componentes y cantidades que componen la configuración")
    @Builder.Default
    private List<@Valid ItemConfiguracionDto> items = new ArrayList<>();
}
