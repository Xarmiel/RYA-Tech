package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CalcularConfiguracionRequestDto;
import com.ecommerce.backend.dto.CalcularConfiguracionResponseDto;
import com.ecommerce.backend.dto.AplicarCambioConfiguracionRequestDto;
import com.ecommerce.backend.dto.AplicarCambioConfiguracionResponseDto;
import com.ecommerce.backend.dto.ConfiguracionPcDto;
import com.ecommerce.backend.dto.CrearConfiguracionRequestDto;
import com.ecommerce.backend.dto.PcBuilderInicialDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.dto.ValidarCambioRequestDto;
import com.ecommerce.backend.dto.ValidarCambioResponseDto;
import com.ecommerce.backend.service.PcBuilderService;
import com.ecommerce.backend.service.ConfiguracionPcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pc-builder")
@RequiredArgsConstructor
@Tag(name = "Asesoría / PC Builder", description = "Motor de validación de compatibilidad técnica, estimación de consumo y cálculo de presupuestos")
public class PcBuilderController {

    private final PcBuilderService pcBuilderService;
    private final ConfiguracionPcService configuracionPcService;

    @GetMapping("/inicial")
    @Operation(summary = "Cargar catálogos iniciales del configurador",
            description = "Devuelve los tipos de componente existentes en la BD. No genera perfiles ni resoluciones demostrativas.")
    public ResponseEntity<PcBuilderInicialDto> inicial() {
        return ResponseEntity.ok(pcBuilderService.obtenerDatosIniciales());
    }

    @GetMapping("/opciones")
    @Operation(summary = "Listar opciones de un paso del configurador",
            description = "Devuelve productos reales, activos y vendibles del tipo solicitado; puede restringir placas usando los componentes ya elegidos.")
    public ResponseEntity<List<ProductoResumenDto>> opciones(
            @RequestParam String tipo,
            @RequestParam(required = false) List<UUID> componentesActuales
    ) {
        return ResponseEntity.ok(pcBuilderService.obtenerComponentesCompatibles(tipo, componentesActuales));
    }

    @PostMapping("/validar-cambio")
    @Operation(summary = "Validar cambio de componente o compatibilidad",
            description = "Evalúa CPU/placa con soporte oficial y BIOS, además de RAM, refrigeración, GPU, fuente, gabinete y componentes bloqueados usando las fichas técnicas almacenadas.")
    public ResponseEntity<ValidarCambioResponseDto> validarCambio(
            @Valid @RequestBody ValidarCambioRequestDto request
    ) {
        return ResponseEntity.ok(pcBuilderService.validarCambio(request));
    }

    @PostMapping("/calcular")
    @Operation(summary = "Calcular total, consumo y fuente recomendada",
            description = "Calcula el costo total de la configuración en BigDecimal, el consumo eléctrico total estimado (Watts), recomienda la potencia de la fuente de poder y verifica la disponibilidad de stock.")
    public ResponseEntity<CalcularConfiguracionResponseDto> calcular(
            @Valid @RequestBody CalcularConfiguracionRequestDto request
    ) {
        return ResponseEntity.ok(pcBuilderService.calcularConfiguracion(request));
    }

    @PostMapping("/configuraciones")
    @Operation(summary = "Crear una configuración persistente",
            description = "Crea un borrador vacío en la base de datos. Las preferencias visuales continúan siendo responsabilidad del frontend/localStorage.")
    public ResponseEntity<ConfiguracionPcDto> crearConfiguracion(
            Authentication authentication,
            @Valid @RequestBody CrearConfiguracionRequestDto request
    ) {
        return ResponseEntity.ok(configuracionPcService.crear(usuarioId(authentication), request));
    }

    @GetMapping("/configuraciones")
    @Operation(summary = "Listar las configuraciones del usuario autenticado")
    public ResponseEntity<List<ConfiguracionPcDto>> listarConfiguraciones(Authentication authentication) {
        return ResponseEntity.ok(configuracionPcService.listar(usuarioId(authentication)));
    }

    @GetMapping("/configuraciones/{id}")
    @Operation(summary = "Consultar una configuración persistente")
    public ResponseEntity<ConfiguracionPcDto> obtenerConfiguracion(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(configuracionPcService.obtener(usuarioId(authentication), id));
    }

    @PostMapping("/aplicar-cambio")
    @Operation(summary = "Aplicar un cambio a una configuración",
            description = "Revalida producto, precio, presupuesto y compatibilidad antes de guardar. Nunca sustituye componentes bloqueados.")
    public ResponseEntity<AplicarCambioConfiguracionResponseDto> aplicarCambio(
            Authentication authentication,
            @Valid @RequestBody AplicarCambioConfiguracionRequestDto request
    ) {
        return ResponseEntity.ok(configuracionPcService.aplicarCambio(usuarioId(authentication), request));
    }

    private UUID usuarioId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
