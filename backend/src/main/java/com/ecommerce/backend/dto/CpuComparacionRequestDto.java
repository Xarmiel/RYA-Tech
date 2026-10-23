package com.ecommerce.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuComparacionRequestDto {

    @NotEmpty(message = "Debe proporcionar al menos un procesador para comparar")
    @Size(min = 1, max = 3, message = "Se permite comparar como máximo 3 procesadores simultáneamente")
    @Schema(description = "Lista de 1 a 3 IDs de procesadores a comparar", example = "[\"a0000000-0000-0000-0000-000000000001\", \"a0000000-0000-0000-0000-000000000002\"]")
    @Builder.Default
    private List<UUID> cpuIds = new ArrayList<>();
}
