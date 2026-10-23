package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcBuilderInicialDto {
    @Builder.Default
    private List<Map<String, Object>> profileFamilies = new ArrayList<>();

    @Builder.Default
    private List<TipoComponenteResumenDto> componentTypes = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> resolutions = new ArrayList<>();
}
