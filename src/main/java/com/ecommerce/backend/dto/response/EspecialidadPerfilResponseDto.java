package com.ecommerce.backend.dto.response;

public record EspecialidadPerfilResponseDto(
    Integer especialidadId,
    String nombre,
    String campo,
    Integer perfilId,
    Short prioCpu,
    Short prioGpu,
    Short prioDisco,
    Short prioRam,
    Short prioFuente,
    Short prioPlaca,
    Short prioCase,
    String descripcion
) {}
