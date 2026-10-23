package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.FiltrosDisponiblesDto;

public interface FiltroService {
    FiltrosDisponiblesDto obtenerFiltrosDisponibles(String categoriaSlug);
}
