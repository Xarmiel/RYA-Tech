package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AdminProductoDetalleDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoDetalleDto;
import com.ecommerce.backend.dto.ProductoResumenDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductoService {

    PaginatedResponseDto<ProductoResumenDto> listarProductos(
            String busqueda,
            UUID categoriaId,
            String categoriaSlug,
            UUID marcaId,
            String marcaSlug,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Boolean soloEnStock,
            Map<String, List<String>> filtrosAtributos,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ProductoDetalleDto obtenerPorId(UUID id);

    ProductoDetalleDto obtenerPorSlug(String slug);

    AdminProductoDetalleDto obtenerPorIdAdmin(UUID id);

    PaginatedResponseDto<ProductoResumenDto> listarProductosAdmin(
            String busqueda,
            UUID categoriaId,
            UUID marcaId,
            String estado,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminProductoDetalleDto crearProducto(com.ecommerce.backend.dto.AdminProductoDto dto);

    AdminProductoDetalleDto actualizarProducto(UUID id, com.ecommerce.backend.dto.AdminProductoDto dto);

    AdminProductoDetalleDto cambiarEstado(UUID id, String nuevoEstado);

    void eliminarProducto(UUID id);
}
