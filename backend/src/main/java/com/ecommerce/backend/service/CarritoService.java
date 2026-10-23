package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;

import java.util.UUID;

public interface CarritoService {
    CarritoDto obtener(String sessionKey, UUID usuarioId);
    CarritoDto agregar(String sessionKey, UUID usuarioId, AgregarItemCarritoRequestDto request);
    CarritoDto actualizar(String sessionKey, UUID usuarioId, UUID itemId, ActualizarItemCarritoRequestDto request);
    CarritoDto eliminar(String sessionKey, UUID usuarioId, UUID itemId);
    CarritoDto vincular(String sessionKey, UUID usuarioId);
}
