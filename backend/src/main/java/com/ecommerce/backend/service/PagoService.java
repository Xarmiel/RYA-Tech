package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.pago.*;
import java.util.UUID;

public interface PagoService {
    IniciarPagoResponseDto iniciarPago(String sessionKey, UUID usuarioId, CrearPedidoPagoRequestDto request);
    ResultadoPagoDto procesarCargo(String sessionKey, UUID usuarioId, ProcesarCargoRequestDto request);
    ResultadoPagoDto consultarPago(UUID pedidoId,UUID usuarioId);
    void procesarWebhook(String eventPayload,String signature,String requestId,String dataId);
}
