package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;

import java.util.UUID;

public interface ConfiguracionPcService {
    ConfiguracionPcDto crear(UUID usuarioId, CrearConfiguracionRequestDto request);
    java.util.List<ConfiguracionPcDto> listar(UUID usuarioId);
    ConfiguracionPcDto obtener(UUID usuarioId, UUID id);
    AplicarCambioConfiguracionResponseDto aplicarCambio(UUID usuarioId, AplicarCambioConfiguracionRequestDto request);
}
