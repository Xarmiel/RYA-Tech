package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ResenasProductoDto;
import com.ecommerce.backend.repository.ResenaProductoRepository;
import com.ecommerce.backend.service.impl.ResenaProductoServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResenaProductoServiceTest {
    @Test
    void calculaResumenSoloConResenasPublicadasDevueltasPorRepositorio() {
        ResenaProductoRepository repository = mock(ResenaProductoRepository.class);
        UUID productoId = UUID.randomUUID();
        ResenaProductoRepository.ResenaPublicadaProjection primera = review((short) 5);
        ResenaProductoRepository.ResenaPublicadaProjection segunda = review((short) 4);
        when(repository.findPublicadasByProductoId(productoId)).thenReturn(List.of(primera, segunda));

        ResenasProductoDto resultado = new ResenaProductoServiceImpl(repository).listarPublicadas(productoId);

        assertEquals(2, resultado.getTotal());
        assertEquals("4.5", resultado.getPromedio().toPlainString());
    }

    private ResenaProductoRepository.ResenaPublicadaProjection review(short rating) {
        ResenaProductoRepository.ResenaPublicadaProjection item = mock(ResenaProductoRepository.ResenaPublicadaProjection.class);
        when(item.getId()).thenReturn(UUID.randomUUID());
        when(item.getCalificacion()).thenReturn(rating);
        when(item.getAutor()).thenReturn("Cliente");
        when(item.getFecha()).thenReturn(OffsetDateTime.now());
        return item;
    }
}
