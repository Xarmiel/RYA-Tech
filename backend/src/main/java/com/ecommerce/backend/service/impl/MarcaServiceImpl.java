package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.CrearMarcaDto;
import com.ecommerce.backend.dto.MarcaDto;
import com.ecommerce.backend.entity.Marca;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.MarcaRepository;
import com.ecommerce.backend.service.MarcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository marcaRepository;

    @Override
    public List<MarcaDto> listarActivas() {
        return marcaRepository.findByIsDeletedFalseOrderByNombreAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public MarcaDto obtenerPorSlug(String slug) {
        Marca marca = marcaRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada con slug: " + slug));
        return toDto(marca);
    }

    @Override
    @Transactional
    public MarcaDto crearMarca(CrearMarcaDto dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new BadRequestException("El nombre de la marca es obligatorio");
        }
        String nombre = dto.getNombre().trim();

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
            slug = "marca-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (marcaRepository.existsBySlugAndIsDeletedFalse(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        Marca marca = Marca.builder()
                .nombre(nombre)
                .slug(slug)
                .logoUrl(dto.getLogoUrl())
                .descripcion(dto.getDescripcion())
                .isDeleted(false)
                .build();

        marca = marcaRepository.save(marca);
        return toDto(marca);
    }

    private MarcaDto toDto(Marca marca) {
        if (marca == null) return null;
        return MarcaDto.builder()
                .id(marca.getId())
                .nombre(marca.getNombre())
                .slug(marca.getSlug())
                .logoUrl(marca.getLogoUrl())
                .descripcion(marca.getDescripcion())
                .build();
    }
}
