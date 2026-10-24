package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CategoriaDto;
import com.ecommerce.backend.entity.Categoria;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.mapper.CategoriaMapper;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {
    @Test
    void rechazaNombreExistenteSinCrearOtraCategoria() {
        when(categoriaRepository.existsByNombreIgnoreCaseAndIsDeletedFalse("Procesadores")).thenReturn(true);
        var dto = com.ecommerce.backend.dto.CrearCategoriaDto.builder().nombre(" Procesadores ").build();
        assertThrows(com.ecommerce.backend.exception.BadRequestException.class, () -> categoriaService.crearCategoria(dto));
        verify(categoriaRepository, never()).save(any());
    }

    @Mock
    private CategoriaRepository categoriaRepository;

    private final CategoriaMapper categoriaMapper = new CategoriaMapper();

    private CategoriaServiceImpl categoriaService;

    private Categoria cat1;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaServiceImpl(categoriaRepository, categoriaMapper);
        cat1 = Categoria.builder()
                .id(UUID.randomUUID())
                .nombre("Procesadores")
                .slug("procesadores")
                .acronimo("CPU")
                .posicion(1)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Debe listar únicamente categorías activas y no eliminadas")
    void testListarActivas() {
        when(categoriaRepository.findByIsDeletedFalseOrderByPosicionAsc()).thenReturn(List.of(cat1));

        List<CategoriaDto> result = categoriaService.listarActivas();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Procesadores", result.get(0).getNombre());
        assertEquals("procesadores", result.get(0).getSlug());
        verify(categoriaRepository, times(1)).findByIsDeletedFalseOrderByPosicionAsc();
    }

    @Test
    @DisplayName("Debe obtener categoría por slug existente")
    void testObtenerPorSlugExitoso() {
        when(categoriaRepository.findBySlugAndIsDeletedFalse("procesadores")).thenReturn(Optional.of(cat1));

        CategoriaDto dto = categoriaService.obtenerPorSlug("procesadores");

        assertNotNull(dto);
        assertEquals("Procesadores", dto.getNombre());
        assertEquals("procesadores", dto.getSlug());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el slug no existe")
    void testObtenerPorSlugNoExiste() {
        when(categoriaRepository.findBySlugAndIsDeletedFalse("inexistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.obtenerPorSlug("inexistente"));
    }

    @Test
    @DisplayName("Debe crear una categoría nueva con slug autogenerado y acrónimo")
    void testCrearCategoriaExitoso() {
        when(categoriaRepository.existsBySlugAndIsDeletedFalse("perifericos")).thenReturn(false);
        when(categoriaRepository.count()).thenReturn(5L);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        com.ecommerce.backend.dto.CrearCategoriaDto dto = com.ecommerce.backend.dto.CrearCategoriaDto.builder()
                .nombre("Periféricos")
                .acronimo("PER")
                .build();

        CategoriaDto creada = categoriaService.crearCategoria(dto);

        assertNotNull(creada);
        assertEquals("Periféricos", creada.getNombre());
        assertEquals("perifericos", creada.getSlug());
        assertEquals("PER", creada.getAcronimo());
        assertEquals(6, creada.getPosicion());
    }

    @Test
    @DisplayName("Debe rechazar categoría que intente ser su propio padre")
    void testCrearCategoriaSelfParentRechazado() {
        UUID parentId = UUID.randomUUID();
        when(categoriaRepository.existsBySlugAndIsDeletedFalse("monitores")).thenReturn(false);
        when(categoriaRepository.findById(parentId)).thenReturn(Optional.of(
                Categoria.builder().id(parentId).nombre("Monitores").slug("monitores").build()
        ));

        com.ecommerce.backend.dto.CrearCategoriaDto dto = com.ecommerce.backend.dto.CrearCategoriaDto.builder()
                .nombre("Monitores")
                .slug("monitores")
                .parentId(parentId)
                .build();

        assertThrows(com.ecommerce.backend.exception.BadRequestException.class, () -> categoriaService.crearCategoria(dto));
    }
}
