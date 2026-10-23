package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CrearMarcaDto;
import com.ecommerce.backend.dto.MarcaDto;
import com.ecommerce.backend.entity.Marca;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.MarcaRepository;
import com.ecommerce.backend.service.impl.MarcaServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarcaServiceTest {

    @Mock
    private MarcaRepository marcaRepository;

    private MarcaServiceImpl marcaService;

    private Marca marca1;

    @BeforeEach
    void setUp() {
        marcaService = new MarcaServiceImpl(marcaRepository);
        marca1 = Marca.builder()
                .id(UUID.randomUUID())
                .nombre("Corsair")
                .slug("corsair")
                .logoUrl("https://example.com/corsair.png")
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Debe listar marcas activas ordenadas")
    void testListarActivas() {
        when(marcaRepository.findByIsDeletedFalseOrderByNombreAsc()).thenReturn(List.of(marca1));

        List<MarcaDto> list = marcaService.listarActivas();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Corsair", list.get(0).getNombre());
    }

    @Test
    @DisplayName("Debe obtener marca por slug")
    void testObtenerPorSlug() {
        when(marcaRepository.findBySlugAndIsDeletedFalse("corsair")).thenReturn(Optional.of(marca1));

        MarcaDto dto = marcaService.obtenerPorSlug("corsair");

        assertNotNull(dto);
        assertEquals("Corsair", dto.getNombre());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el slug de marca no existe")
    void testObtenerPorSlugNoExiste() {
        when(marcaRepository.findBySlugAndIsDeletedFalse("inexistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> marcaService.obtenerPorSlug("inexistente"));
    }

    @Test
    @DisplayName("Debe crear una nueva marca con slug automático")
    void testCrearMarca() {
        when(marcaRepository.existsBySlugAndIsDeletedFalse("logitech-g")).thenReturn(false);
        when(marcaRepository.save(any(Marca.class))).thenAnswer(inv -> {
            Marca m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        CrearMarcaDto dto = CrearMarcaDto.builder()
                .nombre("Logitech G")
                .logoUrl("https://example.com/logitech.png")
                .descripcion("Periféricos gaming")
                .build();

        MarcaDto creada = marcaService.crearMarca(dto);

        assertNotNull(creada);
        assertEquals("Logitech G", creada.getNombre());
        assertEquals("logitech-g", creada.getSlug());
    }

    @Test
    @DisplayName("Debe rechazar creación de marca sin nombre")
    void testCrearMarcaSinNombre() {
        CrearMarcaDto dto = CrearMarcaDto.builder().nombre("  ").build();

        assertThrows(BadRequestException.class, () -> marcaService.crearMarca(dto));
    }
}
