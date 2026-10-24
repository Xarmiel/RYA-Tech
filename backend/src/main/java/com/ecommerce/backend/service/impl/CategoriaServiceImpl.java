package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.CategoriaDto;
import com.ecommerce.backend.dto.CrearCategoriaDto;
import com.ecommerce.backend.entity.Categoria;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.mapper.CategoriaMapper;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    public List<CategoriaDto> listarActivas() {
        List<Categoria> categorias = categoriaRepository.findByIsDeletedFalseOrderByPosicionAsc();
        return categoriaMapper.toDtoList(categorias);
    }

    @Override
    public CategoriaDto obtenerPorSlug(String slug) {
        Categoria categoria = categoriaRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con slug: " + slug));
        return categoriaMapper.toDto(categoria);
    }

    @Override
    @Transactional
    public CategoriaDto crearCategoria(CrearCategoriaDto dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new BadRequestException("El nombre de la categoría es obligatorio");
        }
        String nombre = dto.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIsDeletedFalse(nombre)) {
            throw new BadRequestException("Ya existe una categoría con ese nombre");
        }
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BadRequestException("Esa categoría está eliminada. Activa Mostrar eliminadas y restáurala en Marcas y categorías.");
        }

        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
        } else {
            slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT)
                    .trim()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
        }
        if (slug.isBlank()) {
            slug = "cat-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (categoriaRepository.existsBySlugAndIsDeletedFalse(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 4);
        }
        if (categoriaRepository.existsBySlug(slug)) {
            throw new BadRequestException("El slug ya está reservado por una categoría. Revisa las eliminadas o elige otro texto de enlace.");
        }

        UUID parentId = dto.getParentId();
        if (parentId != null) {
            Categoria parent = categoriaRepository.findById(parentId)
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .orElseThrow(() -> new BadRequestException("La categoría superior no existe"));
            if (parent.getSlug().equalsIgnoreCase(slug)) {
                throw new BadRequestException("Una categoría no puede ser su propia categoría superior");
            }
        }

        String acronimo = dto.getAcronimo();
        if (acronimo != null && !acronimo.isBlank()) {
            acronimo = acronimo.trim().toUpperCase(Locale.ROOT);
        } else {
            acronimo = nombre.length() >= 3 ? nombre.substring(0, 3).toUpperCase(Locale.ROOT) : nombre.toUpperCase(Locale.ROOT);
        }

        int posicion = dto.getPosicion() != null ? dto.getPosicion() : (int) categoriaRepository.count() + 1;

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .slug(slug)
                .acronimo(acronimo)
                .parentId(parentId)
                .posicion(posicion)
                .isDeleted(false)
                .build();

        categoria = categoriaRepository.save(categoria);
        return categoriaMapper.toDto(categoria);
    }
}
