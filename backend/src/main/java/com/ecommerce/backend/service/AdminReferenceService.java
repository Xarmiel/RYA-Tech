package com.ecommerce.backend.service;

import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.repository.MarcaRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReferenceService {
    private final CategoriaRepository categorias;
    private final MarcaRepository marcas;
    private final ProductoRepository productos;
    public record Item(UUID id, String nombre, String slug, boolean eliminada) {}
    public record Listado(List<Item> categorias, List<Item> marcas) {}

    public Listado listar(boolean incluirEliminadas) {
        var cats = categorias.findAll().stream()
                .filter(c -> incluirEliminadas || !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(c -> new Item(c.getId(), c.getNombre(), c.getSlug(), Boolean.TRUE.equals(c.getIsDeleted())))
                .sorted(Comparator.comparing(Item::nombre, String.CASE_INSENSITIVE_ORDER)).toList();
        var brands = marcas.findAll().stream()
                .filter(m -> incluirEliminadas || !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(m -> new Item(m.getId(), m.getNombre(), m.getSlug(), Boolean.TRUE.equals(m.getIsDeleted())))
                .sorted(Comparator.comparing(Item::nombre, String.CASE_INSENSITIVE_ORDER)).toList();
        return new Listado(cats, brands);
    }

    @Transactional
    public void eliminarMarca(UUID id) {
        var marca = marcas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        if (Boolean.TRUE.equals(marca.getIsDeleted())) return;
        if (productos.existsByMarca_Id(id)) throw new BadRequestException("No se puede borrar: la marca tiene productos asociados, incluidos los archivados. Reasigna sus productos primero.");
        marca.setIsDeleted(true);
        marcas.save(marca);
    }

    @Transactional
    public void eliminarCategoria(UUID id) {
        var categoria = categorias.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        if (Boolean.TRUE.equals(categoria.getIsDeleted())) return;
        if (productos.existsByCategoria_Id(id)) throw new BadRequestException("No se puede borrar: la categoría tiene productos asociados, incluidos los archivados. Reasigna sus productos primero.");
        if (categorias.existsByParentIdAndIsDeletedFalse(id)) throw new BadRequestException("No se puede borrar una categoría con subcategorías activas.");
        categoria.setIsDeleted(true);
        categorias.save(categoria);
    }

    @Transactional
    public void restaurarMarca(UUID id) {
        var marca = marcas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        if (!Boolean.TRUE.equals(marca.getIsDeleted())) return;
        if (marcas.existsByNombreIgnoreCaseAndIsDeletedFalse(marca.getNombre()) || marcas.existsBySlugAndIsDeletedFalse(marca.getSlug()))
            throw new BadRequestException("Ya existe una marca activa con ese nombre o slug.");
        marca.setIsDeleted(false); marcas.save(marca);
    }

    @Transactional
    public void restaurarCategoria(UUID id) {
        var categoria = categorias.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        if (!Boolean.TRUE.equals(categoria.getIsDeleted())) return;
        if (categorias.existsByNombreIgnoreCaseAndIsDeletedFalse(categoria.getNombre()) || categorias.existsBySlugAndIsDeletedFalse(categoria.getSlug()))
            throw new BadRequestException("Ya existe una categoría activa con ese nombre o slug.");
        if (categoria.getParentId() != null && categorias.findById(categoria.getParentId()).filter(c -> !Boolean.TRUE.equals(c.getIsDeleted())).isEmpty())
            throw new BadRequestException("Restaura primero la categoría superior.");
        categoria.setIsDeleted(false); categorias.save(categoria);
    }
}
