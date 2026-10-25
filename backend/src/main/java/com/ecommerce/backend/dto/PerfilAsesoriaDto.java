package com.ecommerce.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilAsesoriaDto {
    private UUID id;

    @NotNull(message = "La familia de perfil es requerida")
    private UUID familiaId;
    private String familiaNombre;
    private String familiaCodigo;

    @NotBlank(message = "El código del perfil es requerido")
    private String codigo;

    @NotBlank(message = "El nombre del perfil es requerido")
    private String nombre;

    private String descripcion;

    @Builder.Default
    private Integer prioridadCpu = 5;

    @Builder.Default
    private Integer prioridadGpu = 5;

    @Builder.Default
    private Integer ramMinimaGb = 8;

    @Builder.Default
    private Boolean requiereGpuDedicada = false;

    @DecimalMin(value = "0.00", message = "El presupuesto mínimo no puede ser negativo")
    private BigDecimal presupuestoSugeridoMin;

    @DecimalMin(value = "0.00", message = "El presupuesto máximo no puede ser negativo")
    private BigDecimal presupuestoSugeridoMax;

    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    private List<ReglaPresupuestoDto> reglasPresupuesto = new ArrayList<>();

    @Builder.Default
    private List<NecesidadPerfilDto> necesidades = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReglaPresupuestoDto {
        private UUID id;
        @NotBlank(message = "El código de componente es requerido")
        private String tipoComponenteCodigo;
        private Integer prioridad;
        private BigDecimal porcentajeMin;
        private BigDecimal porcentajeMax;
        @Builder.Default
        private Boolean obligatorio = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NecesidadPerfilDto {
        private UUID id;
        private String codigo;
        @NotBlank(message = "El nombre de la necesidad es requerido")
        private String nombre;
        private String descripcion;
    }
}
