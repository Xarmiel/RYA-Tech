package com.ecommerce.backend.service.implement;

import com.ecommerce.backend.dto.request.ProductoCreateDto;
import com.ecommerce.backend.dto.response.ProductoResponseDto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.mapper.ProductoMapper;
import com.ecommerce.backend.model.Categoria;
import com.ecommerce.backend.model.Producto;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional
    public ProductoResponseDto crearProducto(ProductoCreateDto dto) {
        if (productoRepository.findBySku(dto.sku()).isPresent()) {
            throw new BadRequestException("Ya existe un producto con el SKU: " + dto.sku());
        }

        Categoria subcategoria = categoriaRepository.findById(dto.subcategoriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Subcategoría no encontrada con ID: " + dto.subcategoriaId()));

        Producto producto = productoMapper.toEntity(dto, subcategoria);
        return productoMapper.toResponseDto(productoRepository.save(producto));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDto obtenerPorId(UUID id) {
        return productoRepository.findByIdWithEspecificaciones(id)
            .map(productoMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDto obtenerPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BadRequestException("El nombre del producto no puede estar vacío");
        }
        return productoRepository.findByNombreIgnoreCaseAndActivoTrue(nombre.trim())
            .map(productoMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDto> buscarPorNombre(String query) {
        if (query == null || query.trim().isEmpty()) {
            return listarActivos();
        }
        return productoRepository.buscarPorNombreContiene(query.trim()).stream()
            .map(productoMapper::toResponseDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDto> listarActivos() {
        return productoRepository.findByActivoTrue().stream()
            .map(productoMapper::toResponseDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDto> listarPorCategoria(Integer subcategoriaId) {
        return productoRepository.findByCategoriaId(subcategoriaId).stream()
            .map(productoMapper::toResponseDto)
            .toList();
    }

    @Override
    @Transactional
    public ProductoResponseDto actualizarProducto(UUID id, com.ecommerce.backend.dto.request.ProductoUpdateDto dto) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        producto.setNombre(dto.nombre().trim());
        producto.setPrecioBase(dto.precioBase());
        producto.setImagenUrl(dto.imagenUrl().trim());

        return productoMapper.toResponseDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void desactivarProducto(UUID id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public void desactivarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BadRequestException("El nombre del producto no puede estar vacío");
        }
        Producto producto = productoRepository.findByNombreIgnoreCaseAndActivoTrue(nombre.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}