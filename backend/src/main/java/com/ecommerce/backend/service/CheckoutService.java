package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CheckoutOpcionesDto;
import com.ecommerce.backend.dto.CheckoutValidacionDto;
import com.ecommerce.backend.dto.ValidarCheckoutRequestDto;

import java.util.UUID;

public interface CheckoutService {
    CheckoutOpcionesDto obtenerOpciones();
    CheckoutValidacionDto validar(String sessionKey, UUID usuarioId, ValidarCheckoutRequestDto request);
}
