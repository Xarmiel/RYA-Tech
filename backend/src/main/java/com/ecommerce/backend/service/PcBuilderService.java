package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;

import java.util.List;
import java.util.UUID;

public interface PcBuilderService {

    ComponenteTipoDto listarComponentesPorTipo(String tipo);

    PcBuilderInicialDto obtenerDatosIniciales();

    List<ProductoResumenDto> obtenerComponentesCompatibles(String tipo, List<UUID> componentesActuales);

    ValidarCambioResponseDto validarCambio(ValidarCambioRequestDto request);

    CalcularConfiguracionResponseDto calcularConfiguracion(CalcularConfiguracionRequestDto request);
}
