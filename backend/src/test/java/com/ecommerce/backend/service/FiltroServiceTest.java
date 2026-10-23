package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.FiltrosDisponiblesDto;
import com.ecommerce.backend.entity.AtributoTecnico;
import com.ecommerce.backend.entity.Categoria;
import com.ecommerce.backend.entity.Marca;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.impl.FiltroServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltroServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MarcaRepository marcaRepository;
    @Mock
    private AtributoTecnicoRepository atributoTecnicoRepository;
    @Mock
    private ValorAtributoProductoRepository valorAtributoProductoRepository;

    @InjectMocks
    private FiltroServiceImpl filtroService;

    @Test
    @DisplayName("Debe construir filtros dinámicos desde la BD sin valores inventados o hardcodeados")
    void testObtenerFiltrosDisponibles() {
        UUID catId = UUID.randomUUID();
        UUID marcaId = UUID.randomUUID();
        UUID atId = UUID.randomUUID();

        List<Object[]> catCounts = Collections.singletonList(new Object[]{catId, 5L});
        when(productoRepository.countProductosPorCategoria()).thenReturn(catCounts);
        when(categoriaRepository.findAllById(any())).thenReturn(List.of(
                Categoria.builder().id(catId).nombre("Procesadores").slug("procesadores").isDeleted(false).build()
        ));

        List<Object[]> marcaCounts = Collections.singletonList(new Object[]{marcaId, 3L});
        when(categoriaRepository.findBySlugAndIsDeletedFalse("procesadores")).thenReturn(java.util.Optional.of(
                Categoria.builder().id(catId).nombre("Procesadores").slug("procesadores").isDeleted(false).build()
        ));
        when(productoRepository.countProductosPorMarcaYCategoria(catId)).thenReturn(marcaCounts);
        when(marcaRepository.findAllById(any())).thenReturn(List.of(
                Marca.builder().id(marcaId).nombre("AMD").slug("amd").isDeleted(false).build()
        ));

        when(productoRepository.findMinPrecioActivoPorCategoria(catId)).thenReturn(new BigDecimal("150.00"));
        when(productoRepository.findMaxPrecioActivoPorCategoria(catId)).thenReturn(new BigDecimal("2500.00"));

        AtributoTecnico at = AtributoTecnico.builder()
                .id(atId)
                .codigo("socket")
                .nombre("Socket")
                .tipoDato("TEXTO")
                .filtrable(true)
                .isDeleted(false)
                .build();
        when(atributoTecnicoRepository.findByFiltrableTrueAndIsDeletedFalseOrderByNombreAsc())
                .thenReturn(List.of(at));
        when(valorAtributoProductoRepository.findValoresTextoDistintosPorCategoria(atId, catId))
                .thenReturn(List.of("AM4", "AM5"));

        FiltrosDisponiblesDto filtros = filtroService.obtenerFiltrosDisponibles("procesadores");

        assertNotNull(filtros);
        assertEquals(1, filtros.getCategorias().size());
        assertEquals("Procesadores", filtros.getCategorias().get(0).getNombre());

        assertEquals(1, filtros.getMarcas().size());
        assertEquals("AMD", filtros.getMarcas().get(0).getNombre());

        assertEquals(new BigDecimal("150.00"), filtros.getRangoPrecio().getMin());
        assertEquals(new BigDecimal("2500.00"), filtros.getRangoPrecio().getMax());

        assertEquals(1, filtros.getAtributos().size());
        assertEquals("socket", filtros.getAtributos().get(0).getCodigo());
        assertEquals(List.of("AM4", "AM5"), filtros.getAtributos().get(0).getValores());
    }
}
