package com.ecommerce.backend.controller;

import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Order(-1)
@RestControllerAdvice(assignableTypes = AdminInventarioController.class)
public class InventarioAdvice {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<?> invalidBody(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Solicitud inválida: cantidades y versiones deben ser enteros JSON."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "Solicitud rechazada" : ex.getReason()));
    }
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<?> conflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("message", "El inventario cambió. Consulta el stock actual antes de reintentar."));
    }
}
