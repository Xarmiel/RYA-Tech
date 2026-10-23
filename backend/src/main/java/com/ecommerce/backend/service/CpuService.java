package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CpuComparacionRequestDto;
import com.ecommerce.backend.dto.CpuComparacionResponseDto;
import com.ecommerce.backend.dto.CpuDetalleDto;
import com.ecommerce.backend.dto.CpuResumenDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CpuService {

    List<CpuResumenDto> listarCpus(String marca, String socket, String familia, BigDecimal precioMin, BigDecimal precioMax);

    CpuDetalleDto obtenerCpuPorId(UUID id);

    List<CpuResumenDto> obtenerAlternativas(UUID id);

    CpuComparacionResponseDto compararCpus(CpuComparacionRequestDto request);
}
