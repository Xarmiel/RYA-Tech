package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ResenasProductoDto;

import java.util.UUID;

public interface ResenaProductoService {
    ResenasProductoDto listarPublicadas(UUID productoId);
}
