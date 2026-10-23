package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CategoriaDto;
import com.ecommerce.backend.dto.CrearCategoriaDto;

import java.util.List;

public interface CategoriaService {
    List<CategoriaDto> listarActivas();
    CategoriaDto obtenerPorSlug(String slug);
    CategoriaDto crearCategoria(CrearCategoriaDto dto);
}
