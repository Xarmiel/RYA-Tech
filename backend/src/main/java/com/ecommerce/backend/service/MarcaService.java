package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CrearMarcaDto;
import com.ecommerce.backend.dto.MarcaDto;

import java.util.List;

public interface MarcaService {
    List<MarcaDto> listarActivas();
    MarcaDto obtenerPorSlug(String slug);
    MarcaDto crearMarca(CrearMarcaDto dto);
}
