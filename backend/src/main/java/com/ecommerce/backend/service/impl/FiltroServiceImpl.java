package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.FiltroAtributoDto;
import com.ecommerce.backend.dto.FiltroOpcionDto;
import com.ecommerce.backend.dto.FiltroRangoPrecioDto;
import com.ecommerce.backend.dto.FiltrosDisponiblesDto;
import com.ecommerce.backend.entity.AtributoTecnico;
import com.ecommerce.backend.entity.Categoria;
import com.ecommerce.backend.entity.Marca;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.FiltroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FiltroServiceImpl implements FiltroService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final AtributoTecnicoRepository atributoTecnicoRepository;
    private final ValorAtributoProductoRepository valorAtributoProductoRepository;

    @Override
    public FiltrosDisponiblesDto obtenerFiltrosDisponibles(String categoriaSlug) {
        // 1. Categorías dinámicas desde productos activos
        List<Object[]> catCounts = productoRepository.countProductosPorCategoria();
        Map<UUID, Long> catConteoMap = new HashMap<>();
        for (Object[] row : catCounts) {
            if (row[0] != null) {
                catConteoMap.put((UUID) row[0], (Long) row[1]);
            }
        }

        List<Categoria> categorias = categoriaRepository.findAllById(catConteoMap.keySet());
        List<FiltroOpcionDto> categoriasFiltro = categorias.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .map(c -> FiltroOpcionDto.builder()
                        .id(c.getId())
                        .nombre(c.getNombre())
                        .slug(c.getSlug())
                        .conteo(catConteoMap.getOrDefault(c.getId(), 0L))
                        .build())
                .sorted(Comparator.comparing(FiltroOpcionDto::getNombre))
                .toList();

        if (categoriaSlug == null || categoriaSlug.isBlank()) {
            return FiltrosDisponiblesDto.builder()
                    .categorias(categoriasFiltro)
                    .marcas(List.of())
                    .rangoPrecio(FiltroRangoPrecioDto.builder()
                            .min(BigDecimal.ZERO)
                            .max(BigDecimal.ZERO)
                            .build())
                    .atributos(List.of())
                    .build();
        }

        Categoria categoriaSeleccionada = categoriaRepository
                .findBySlugAndIsDeletedFalse(categoriaSlug.trim())
                .orElseThrow(() -> new BadRequestException("La categoría indicada no existe o no está disponible"));

        // 2. Marcas dinámicas limitadas a la categoría seleccionada
        List<Object[]> marcaCounts = productoRepository.countProductosPorMarcaYCategoria(categoriaSeleccionada.getId());
        Map<UUID, Long> marcaConteoMap = new HashMap<>();
        for (Object[] row : marcaCounts) {
            if (row[0] != null) {
                marcaConteoMap.put((UUID) row[0], (Long) row[1]);
            }
        }

        List<Marca> marcas = marcaRepository.findAllById(marcaConteoMap.keySet());
        List<FiltroOpcionDto> marcasFiltro = marcas.stream()
                .filter(m -> Boolean.FALSE.equals(m.getIsDeleted()))
                .map(m -> FiltroOpcionDto.builder()
                        .id(m.getId())
                        .nombre(m.getNombre())
                        .slug(m.getSlug())
                        .conteo(marcaConteoMap.getOrDefault(m.getId(), 0L))
                        .build())
                .sorted(Comparator.comparing(FiltroOpcionDto::getNombre))
                .toList();

        // 3. Rango de precio dinámico (min y max)
        BigDecimal minPrecio = productoRepository.findMinPrecioActivoPorCategoria(categoriaSeleccionada.getId());
        BigDecimal maxPrecio = productoRepository.findMaxPrecioActivoPorCategoria(categoriaSeleccionada.getId());
        FiltroRangoPrecioDto rangoPrecio = FiltroRangoPrecioDto.builder()
                .min(minPrecio != null ? minPrecio : BigDecimal.ZERO)
                .max(maxPrecio != null ? maxPrecio : BigDecimal.ZERO)
                .build();

        // 4. Atributos técnicos configurados como filtrables en rya.atributos_tecnicos
        List<AtributoTecnico> atributosTecnicos = atributoTecnicoRepository.findByFiltrableTrueAndIsDeletedFalseOrderByNombreAsc();
        List<FiltroAtributoDto> atributosFiltro = new ArrayList<>();

        for (AtributoTecnico at : atributosTecnicos) {
            List<String> valores;
            if ("NUMERO".equalsIgnoreCase(at.getTipoDato())) {
                valores = valorAtributoProductoRepository.findValoresNumeroDistintosPorCategoria(
                        at.getId(), categoriaSeleccionada.getId());
            } else if ("BOOLEANO".equalsIgnoreCase(at.getTipoDato())) {
                valores = valorAtributoProductoRepository.findValoresBooleanoDistintosPorCategoria(
                        at.getId(), categoriaSeleccionada.getId());
            } else {
                valores = valorAtributoProductoRepository.findValoresTextoDistintosPorCategoria(
                        at.getId(), categoriaSeleccionada.getId());
            }

            if (!valores.isEmpty()) {
                atributosFiltro.add(FiltroAtributoDto.builder()
                        .codigo(at.getCodigo())
                        .nombre(at.getNombre())
                        .tipoDato(at.getTipoDato())
                        .unidad(at.getUnidad())
                        .valores(valores)
                        .build());
            }
        }

        return FiltrosDisponiblesDto.builder()
                .categorias(categoriasFiltro)
                .marcas(marcasFiltro)
                .rangoPrecio(rangoPrecio)
                .atributos(atributosFiltro)
                .build();
    }
}
