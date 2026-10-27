package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ValidarCheckoutRequestDto;
import com.ecommerce.backend.entity.TarifaEnvio;
import com.ecommerce.backend.exception.BadRequestException;
import java.text.Normalizer;
import java.util.stream.Stream;

public final class EntregaRules {
    private EntregaRules() {}
    public static boolean recojo(TarifaEnvio tarifa) { return "RECOJO".equals(tarifa.getModalidad()); }
    private static String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }
    private static boolean matches(String coverage, String destination) {
        return coverage == null || coverage.isBlank() || normalized(coverage).equals(normalized(destination));
    }
    public static void validar(TarifaEnvio tarifa, ValidarCheckoutRequestDto.Entrega entrega) {
        if (Boolean.FALSE.equals(tarifa.getActivo()) || Boolean.TRUE.equals(tarifa.getIsDeleted()))
            throw new BadRequestException("La opción de entrega ya no está disponible.");
        if (recojo(tarifa)) {
            if (tarifa.getDireccionRecojo() == null || tarifa.getDireccionRecojo().isBlank() || tarifa.getHorarioRecojo() == null || tarifa.getHorarioRecojo().isBlank())
                throw new BadRequestException("El punto de recojo no tiene dirección u horario.");
            return;
        }
        if (Stream.of(entrega.getDepartamento(), entrega.getProvincia(), entrega.getDistrito()).anyMatch(v -> v == null || v.isBlank()))
            throw new BadRequestException("Selecciona departamento, provincia y distrito.");
        if (!matches(tarifa.getDepartamento(), entrega.getDepartamento()) || !matches(tarifa.getProvincia(), entrega.getProvincia()) || !matches(tarifa.getDistrito(), entrega.getDistrito()))
            throw new BadRequestException("La tarifa no cubre la ubicación seleccionada.");
        if (entrega.getDireccion() == null || entrega.getDireccion().trim().length() < 5)
            throw new BadRequestException("Ingresa una dirección completa.");
    }
    public static String descripcion(TarifaEnvio tarifa, ValidarCheckoutRequestDto.Entrega entrega) {
        if (recojo(tarifa)) return "Recojo: " + tarifa.getDireccionRecojo() + " · " + tarifa.getHorarioRecojo();
        return Stream.of(entrega.getDepartamento(), entrega.getProvincia(), entrega.getDistrito()).filter(v -> v != null && !v.isBlank()).collect(java.util.stream.Collectors.joining(", "));
    }
}
