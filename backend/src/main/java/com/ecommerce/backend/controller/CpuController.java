package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CpuComparacionRequestDto;
import com.ecommerce.backend.dto.CpuComparacionResponseDto;
import com.ecommerce.backend.dto.CpuDetalleDto;
import com.ecommerce.backend.dto.CpuResumenDto;
import com.ecommerce.backend.service.CpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpu")
@RequiredArgsConstructor
@Tag(name = "Comparador de CPU", description = "Operaciones para consulta de especificaciones de procesadores, alternativas del catálogo y comparación de hasta 3 procesadores")
public class CpuController {

    private final CpuService cpuService;

    @GetMapping
    @Operation(summary = "Listar procesadores activos",
            description = "Devuelve el catálogo de procesadores con sus especificaciones técnicas de rya.especificaciones_cpu y filtros por marca, socket o rango de precio.")
    public ResponseEntity<List<CpuResumenDto>> listarCpus(
            @Parameter(description = "Filtrar por marca (ej: AMD, Intel)") @RequestParam(required = false) String marca,
            @Parameter(description = "Filtrar por socket (ej: AM5, LGA1700)") @RequestParam(required = false) String socket,
            @Parameter(description = "Filtrar por familia (ej: Ryzen 7, Core i7)") @RequestParam(required = false) String familia,
            @Parameter(description = "Precio mínimo") @RequestParam(required = false) BigDecimal precioMin,
            @Parameter(description = "Precio máximo") @RequestParam(required = false) BigDecimal precioMax
    ) {
        return ResponseEntity.ok(cpuService.listarCpus(marca, socket, familia, precioMin, precioMax));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ficha técnica completa de CPU",
            description = "Recupera la ficha de especificaciones técnicas completas, imágenes de producto, inventario y benchmarks registrados.")
    public ResponseEntity<CpuDetalleDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(cpuService.obtenerCpuPorId(id));
    }

    @GetMapping("/{id}/alternativas")
    @Operation(summary = "Obtener procesadores alternativos",
            description = "Devuelve procesadores alternativos del catálogo existentes en la BD con compatibilidad de socket o familia similar.")
    public ResponseEntity<List<CpuResumenDto>> obtenerAlternativas(@PathVariable UUID id) {
        return ResponseEntity.ok(cpuService.obtenerAlternativas(id));
    }

    @PostMapping("/comparar")
    @Operation(summary = "Comparar entre 1 y 3 procesadores",
            description = "Genera una matriz comparativa con diferencias de precio respecto a la referencia, especificaciones lado a lado y benchmarks en común.")
    public ResponseEntity<CpuComparacionResponseDto> comparar(
            @Valid @RequestBody CpuComparacionRequestDto request
    ) {
        return ResponseEntity.ok(cpuService.compararCpus(request));
    }
}
