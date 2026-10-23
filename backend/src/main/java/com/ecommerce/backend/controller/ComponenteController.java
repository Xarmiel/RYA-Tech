package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ComponenteTipoDto;
import com.ecommerce.backend.service.PcBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/componentes")
@RequiredArgsConstructor
@Tag(name = "Componentes", description = "Catálogo de componentes por tipo para el ensamblador y asesoría")
public class ComponenteController {

    private final PcBuilderService pcBuilderService;

    @GetMapping("/{tipo}")
    @Operation(summary = "Listar componentes por tipo",
            description = "Devuelve los productos activos pertenecientes a un tipo de componente (por ejemplo: CPU, PLACA, GPU, RAM, FUENTE, CASE, DISCO).")
    public ResponseEntity<ComponenteTipoDto> listarPorTipo(
            @Parameter(description = "Código del tipo de componente o slug de categoría (ej: CPU, PLACA, procesadores)", example = "CPU")
            @PathVariable String tipo
    ) {
        return ResponseEntity.ok(pcBuilderService.listarComponentesPorTipo(tipo));
    }
}
