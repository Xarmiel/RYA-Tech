package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.ProductoCreateDto;
import com.ecommerce.backend.dto.response.ProductoResponseDto;
import java.util.List;
import java.util.UUID;

public interface ProductoService {
    ProductoResponseDto crearProducto(ProductoCreateDto dto);
    ProductoResponseDto obtenerPorId(UUID id);
    ProductoResponseDto obtenerPorNombre(String nombre);
    List<ProductoResponseDto> buscarPorNombre(String query);
    List<ProductoResponseDto> listarActivos();
    List<ProductoResponseDto> listarPorCategoria(Integer categoriaId);
    ProductoResponseDto actualizarProducto(UUID id, com.ecommerce.backend.dto.request.ProductoUpdateDto dto);
    void desactivarProducto(UUID id);
    void desactivarPorNombre(String nombre);
}