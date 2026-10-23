package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.AdminProductoDetalleDto;
import com.ecommerce.backend.dto.AdminProductoDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.service.CategoriaService;
import com.ecommerce.backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/productos")
@RequiredArgsConstructor
@Tag(name = "Admin - Productos", description = "Gestión de catálogo de productos para administradores")
public class AdminProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final com.ecommerce.backend.repository.CategoriaRepository categoriaRepository;
    private final com.ecommerce.backend.repository.MarcaRepository marcaRepository;

    public record MetadataItemDto(UUID id, String nombre, String slug) {}
    public record AdminProductMetadataDto(java.util.List<MetadataItemDto> categorias, java.util.List<MetadataItemDto> marcas) {}

    @GetMapping("/metadata")
    @Operation(summary = "Obtener metadatos para formularios de producto (categorías y marcas)")
    public ResponseEntity<AdminProductMetadataDto> obtenerMetadata() {
        var categorias = categoriaRepository.findByIsDeletedFalseOrderByPosicionAsc().stream()
                .map(c -> new MetadataItemDto(c.getId(), c.getNombre(), c.getSlug()))
                .toList();
        var marcas = marcaRepository.findByIsDeletedFalseOrderByNombreAsc().stream()
                .map(m -> new MetadataItemDto(m.getId(), m.getNombre(), m.getSlug()))
                .toList();
        return ResponseEntity.ok(new AdminProductMetadataDto(categorias, marcas));
    }

    @PostMapping("/categorias")
    @Operation(summary = "Crear nueva categoría desde el panel de productos")
    public ResponseEntity<MetadataItemDto> crearCategoria(@Valid @RequestBody com.ecommerce.backend.dto.CrearCategoriaDto dto) {
        var cat = categoriaService.crearCategoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MetadataItemDto(cat.getId(), cat.getNombre(), cat.getSlug()));
    }

    @PostMapping("/marcas")
    @Operation(summary = "Crear nueva marca desde el panel de productos")
    public ResponseEntity<MetadataItemDto> crearMarca(@Valid @RequestBody com.ecommerce.backend.dto.CrearMarcaDto dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = dto.getNombre().toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
        }
        if (marcaRepository.existsBySlugAndIsDeletedFalse(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 4);
        }
        var m = marcaRepository.save(com.ecommerce.backend.entity.Marca.builder()
                .nombre(dto.getNombre().trim())
                .slug(slug)
                .logoUrl(dto.getLogoUrl())
                .descripcion(dto.getDescripcion())
                .isDeleted(false)
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MetadataItemDto(m.getId(), m.getNombre(), m.getSlug()));
    }

    @GetMapping
    @Operation(summary = "Listar productos para administración con filtros avanzados")
    public ResponseEntity<PaginatedResponseDto<ProductoResumenDto>> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID marcaId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(productoService.listarProductosAdmin(
                busqueda, categoriaId, marcaId, estado, page, size, sortBy, sortDir
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de producto por ID")
    public ResponseEntity<AdminProductoDetalleDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.obtenerPorIdAdmin(id));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto (borrador o publicado)")
    public ResponseEntity<AdminProductoDetalleDto> crear(@Valid @RequestBody AdminProductoDto dto) {
        AdminProductoDetalleDto creado = productoService.crearProducto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de producto existente")
    public ResponseEntity<AdminProductoDetalleDto> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AdminProductoDto dto
    ) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de producto (publicar, desactivar, borrador)")
    public ResponseEntity<AdminProductoDetalleDto> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam(required = false) String estado,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String estadoFinal = estado != null && !estado.isBlank() ? estado : (body != null ? body.get("estado") : null);
        if (estadoFinal == null || estadoFinal.isBlank()) {
            estadoFinal = "publicar";
        }
        return ResponseEntity.ok(productoService.cambiarEstado(id, estadoFinal));
    }

    @PatchMapping("/{id}/publicar")
    @Operation(summary = "Publicar producto directamente")
    public ResponseEntity<AdminProductoDetalleDto> publicar(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.cambiarEstado(id, "publicar"));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar producto (ocultar del catálogo sin eliminar)")
    public ResponseEntity<AdminProductoDetalleDto> desactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.cambiarEstado(id, "desactivar"));
    }

    @PatchMapping("/{id}/borrador")
    @Operation(summary = "Guardar producto como borrador")
    public ResponseEntity<AdminProductoDetalleDto> borrador(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.cambiarEstado(id, "borrador"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto suavemente (soft delete)")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
