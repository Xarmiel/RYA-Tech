package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.pago.*;
import java.util.UUID;

public interface PagoService {
    IniciarPagoResponseDto iniciarPago(String sessionKey, UUID usuarioId, CrearPedidoPagoRequestDto request);
    ResultadoPagoDto procesarCargo(String sessionKey, UUID usuarioId, ProcesarCargoRequestDto request);
    void procesarWebhook(String eventPayload, String signature);
}
