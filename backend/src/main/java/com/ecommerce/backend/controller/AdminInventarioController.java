package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.AjusteStockRequestDto;
import com.ecommerce.backend.dto.AlmacenDto;
import com.ecommerce.backend.dto.InventarioItemDto;
import com.ecommerce.backend.dto.MovimientoInventarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventario")
@RequiredArgsConstructor
@Tag(name = "Admin - Inventario", description = "Gestión de existencias, almacenes y movimientos de inventario para administradores")
public class AdminInventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    @Operation(summary = "Listar existencias de inventario paginadas con filtros")
    public ResponseEntity<PaginatedResponseDto<InventarioItemDto>> listarInventario(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) String alerta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(inventarioService.listarInventario(busqueda, almacenId, alerta, page, size));
    }

    @GetMapping("/almacenes")
    @Operation(summary = "Listar almacenes activos")
    public ResponseEntity<List<AlmacenDto>> listarAlmacenes() {
        return ResponseEntity.ok(inventarioService.listarAlmacenesActivos());
    }

    @PostMapping("/ajuste")
    @Operation(summary = "Registrar movimiento o ajuste de inventario (ENTRADA, SALIDA, AJUSTE)")
    public ResponseEntity<InventarioItemDto> registrarAjuste(@Valid @RequestBody AjusteStockRequestDto dto) {
        return ResponseEntity.ok(inventarioService.registrarAjuste(dto));
    }

    @GetMapping("/{existenciaId}/kardex")
    @Operation(summary = "Consultar historial de movimientos (Kardex) de una existencia")
    public ResponseEntity<PaginatedResponseDto<MovimientoInventarioDto>> obtenerKardex(@PathVariable UUID existenciaId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(inventarioService.obtenerKardex(existenciaId, page, size));
    }

    @GetMapping("/stock")
    public InventarioItemDto consultarStock(@RequestParam UUID productoId, @RequestParam UUID almacenId,
            @RequestParam(required=false) UUID varianteId) {
        return inventarioService.consultarStock(productoId, almacenId, varianteId);
    }
}
