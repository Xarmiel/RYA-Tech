package com.ecommerce.backend.service.implement;

import com.ecommerce.backend.dto.request.CategoriaCreateDto;
import com.ecommerce.backend.dto.response.CategoriaResponseDto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.mapper.CategoriaMapper;
import com.ecommerce.backend.model.Categoria;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional
    public CategoriaResponseDto crearCategoria(CategoriaCreateDto dto) {
        if (categoriaRepository.findByNombre(dto.nombre().trim()).isPresent() ||
            categoriaRepository.findBySlug(dto.slug().trim()).isPresent()) {
            throw new BadRequestException("Categoria existente");
        }

        Categoria parent = null;
        if (dto.parentId() != null) {
            parent = categoriaRepository.findById(dto.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría padre no encontrada con ID: " + dto.parentId()));
        }

        Categoria categoria = categoriaMapper.toEntity(dto, parent);
        return categoriaMapper.toResponseDto(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDto obtenerPorId(Integer id) {
        return categoriaRepository.findById(id)
            .map(categoriaMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDto obtenerPorSlug(String slug) {
        return categoriaRepository.findBySlug(slug)
            .map(categoriaMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con slug: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDto> listarRaices() {
        return categoriaRepository.findByParentIsNull().stream()
            .map(categoriaMapper::toResponseDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDto> listarTodas() {
        return categoriaRepository.findAll().stream()
            .map(categoriaMapper::toResponseDto)
            .toList();
    }
}