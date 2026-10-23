package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.FiltrosDisponiblesDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoDetalleDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.service.FiltroService;
import com.ecommerce.backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Productos", description = "Operaciones de consulta, búsqueda y filtros dinámicos del catálogo de productos")
public class ProductoController {

    private final ProductoService productoService;
    private final FiltroService filtroService;

    @GetMapping
    @Operation(summary = "Listar productos activos con paginación y filtros",
            description = "Permite buscar por texto libre (nombre, SKU, descripción) y filtrar por categoría, marca, precio y disponibilidad.")
    public ResponseEntity<PaginatedResponseDto<ProductoResumenDto>> listarProductos(
            @Parameter(description = "Texto de búsqueda libre") @RequestParam(required = false) String q,
            @Parameter(description = "ID de la categoría") @RequestParam(required = false) UUID categoriaId,
            @Parameter(description = "Slug de la categoría") @RequestParam(required = false) String categoria,
            @Parameter(description = "ID de la marca") @RequestParam(required = false) UUID marcaId,
            @Parameter(description = "Slug de la marca") @RequestParam(required = false) String marca,
            @Parameter(description = "Precio mínimo referencial") @RequestParam(required = false) @DecimalMin("0.00") BigDecimal precioMin,
            @Parameter(description = "Precio máximo referencial") @RequestParam(required = false) @DecimalMin("0.00") BigDecimal precioMax,
            @Parameter(description = "Filtrar solo productos con stock disponible > 0") @RequestParam(required = false) Boolean enStock,
            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @Parameter(description = "Campo de ordenamiento (nombre, precio, disponibilidad, created)") @RequestParam(defaultValue = "nombre") String sortBy,
            @Parameter(description = "Dirección de orden (asc o desc)") @RequestParam(defaultValue = "asc") String sortDir
            , @RequestParam MultiValueMap<String, String> parametros
    ) {
        Map<String, List<String>> filtrosAtributos = new LinkedHashMap<>();
        parametros.forEach((clave, valores) -> {
            if (clave.startsWith("filtro.") && clave.length() > "filtro.".length()) {
                filtrosAtributos.put(clave.substring("filtro.".length()), valores);
            }
        });
        PaginatedResponseDto<ProductoResumenDto> resultado = productoService.listarProductos(
                q, categoriaId, categoria, marcaId, marca, precioMin, precioMax, enStock,
                filtrosAtributos, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/filtros")
    @Operation(summary = "Obtener filtros dinámicos disponibles",
            description = "Construye los filtros de marcas, categorías, rangos de precio y atributos técnicos a partir de los datos existentes en la BD.")
    public ResponseEntity<FiltrosDisponiblesDto> obtenerFiltrosDisponibles(
            @Parameter(description = "Slug de la categoría cuyos filtros técnicos se requieren")
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(filtroService.obtenerFiltrosDisponibles(categoria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID",
            description = "Devuelve la ficha completa del producto incluyendo imágenes, atributos técnicos y stock en tiempo real.")
    public ResponseEntity<ProductoDetalleDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Obtener producto por slug",
            description = "Devuelve la ficha técnica y comercial completa del producto mediante su URL slug único.")
    public ResponseEntity<ProductoDetalleDto> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(productoService.obtenerPorSlug(slug));
    }
}
