package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.impl.ConfiguracionPcServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionPcServiceTest {

    @Mock ConfiguracionPcRepository configuracionRepository;
    @Mock ComponenteConfiguracionRepository componenteRepository;
    @Mock TipoComponenteRepository tipoComponenteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock ExistenciaRepository existenciaRepository;
    @Mock PcBuilderService pcBuilderService;

    private ConfiguracionPcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfiguracionPcServiceImpl(configuracionRepository, componenteRepository,
                tipoComponenteRepository, productoRepository, existenciaRepository, pcBuilderService);
    }

    @Test
    void creaBorradorSinDatosDemo() {
        when(configuracionRepository.save(any())).thenAnswer(invocation -> {
            ConfiguracionPc entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ConfiguracionPcDto result = service.crear(UUID.randomUUID(), CrearConfiguracionRequestDto.builder()
                .nombre("  PC de trabajo  ")
                .presupuesto(new BigDecimal("5000.00"))
                .moneda("pen")
                .build());

        assertNotNull(result.getId());
        assertEquals("PC de trabajo", result.getNombre());
        assertEquals("PEN", result.getMoneda());
        assertEquals("BORRADOR", result.getEstado());
        assertTrue(result.getComponentes().isEmpty());
    }

    @Test
    void impideSustituirComponenteBloqueado() {
        UUID configId = UUID.randomUUID();
        UUID tipoId = UUID.randomUUID();
        UUID originalId = UUID.randomUUID();
        UUID nuevoId = UUID.randomUUID();
        ConfiguracionPc config = configuracion(configId, "4000.00");
        TipoComponente tipo = TipoComponente.builder().id(tipoId).codigo("CPU").nombre("Procesador").build();
        Categoria categoria = Categoria.builder().id(UUID.randomUUID()).nombre("Procesadores")
                .slug("procesadores").acronimo("CPU").build();
        Producto original = Producto.builder().id(originalId).categoria(categoria).sku("CPU-1")
                .nombre("CPU original").precioBase(new BigDecimal("800.00")).activo(true).vendible(true).build();
        Producto nuevo = Producto.builder().id(nuevoId).categoria(categoria).sku("CPU-2")
                .nombre("CPU nueva").precioBase(new BigDecimal("900.00")).activo(true).vendible(true).build();
        ComponenteConfiguracion componente = ComponenteConfiguracion.builder().id(UUID.randomUUID())
                .configuracion(config).tipoComponente(tipo).producto(original).cantidad(1)
                .bloqueado(true).precioSnapshot(original.getPrecioBase()).build();

        UUID usuarioId = UUID.randomUUID();
        config.setUsuarioId(usuarioId);
        when(configuracionRepository.findByIdAndUsuarioIdAndIsDeletedFalse(configId, usuarioId)).thenReturn(Optional.of(config));
        when(tipoComponenteRepository.findByCodigoIgnoreCaseAndIsDeletedFalse("CPU")).thenReturn(Optional.of(tipo));
        when(productoRepository.findConDetallesPorId(nuevoId)).thenReturn(Optional.of(nuevo));
        when(componenteRepository.findByConfiguracionIdAndIsDeletedFalseOrderByCreatedAtAsc(configId))
                .thenReturn(List.of(componente));

        AplicarCambioConfiguracionRequestDto request = AplicarCambioConfiguracionRequestDto.builder()
                .configuracionId(configId).tipoComponente("CPU").productoId(nuevoId).cantidad(1).build();

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.aplicarCambio(usuarioId, request));
        assertTrue(error.getMessage().contains("bloqueado"));
        verify(componenteRepository, never()).save(any());
    }

    @Test
    void aplicaCambioCompatibleConPrecioVigente() {
        UUID configId = UUID.randomUUID();
        UUID tipoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ConfiguracionPc config = configuracion(configId, "4000.00");
        TipoComponente tipo = TipoComponente.builder().id(tipoId).codigo("CPU").nombre("Procesador").build();
        Categoria categoria = Categoria.builder().id(UUID.randomUUID()).nombre("Procesadores")
                .slug("procesadores").acronimo("CPU").build();
        Producto producto = Producto.builder().id(productoId).categoria(categoria).sku("CPU-1")
                .nombre("CPU real").precioBase(new BigDecimal("850.00")).activo(true).vendible(true).build();

        UUID usuarioId = UUID.randomUUID();
        config.setUsuarioId(usuarioId);
        when(configuracionRepository.findByIdAndUsuarioIdAndIsDeletedFalse(configId, usuarioId)).thenReturn(Optional.of(config));
        when(tipoComponenteRepository.findByCodigoIgnoreCaseAndIsDeletedFalse("CPU")).thenReturn(Optional.of(tipo));
        when(productoRepository.findConDetallesPorId(productoId)).thenReturn(Optional.of(producto));
        when(componenteRepository.findByConfiguracionIdAndIsDeletedFalseOrderByCreatedAtAsc(configId))
                .thenReturn(List.of());
        when(pcBuilderService.validarCambio(any())).thenReturn(ValidarCambioResponseDto.builder()
                .estadoCompatibilidad("POR_VERIFICAR").esCompatible(false).mensajes(List.of("Falta placa")).build());
        when(componenteRepository.save(any())).thenAnswer(invocation -> {
            ComponenteConfiguracion item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });
        when(existenciaRepository.findStockDisponiblePorProductoId(productoId)).thenReturn(3);

        AplicarCambioConfiguracionResponseDto result = service.aplicarCambio(usuarioId,
                AplicarCambioConfiguracionRequestDto.builder().configuracionId(configId)
                        .tipoComponente("CPU").productoId(productoId).cantidad(1).build());

        assertEquals(0, result.getConfiguracion().getTotalEstimado().compareTo(new BigDecimal("850.00")));
        assertEquals(3, result.getConfiguracion().getComponentes().getFirst().getStockDisponible());
        assertEquals("POR_VERIFICAR", result.getCompatibilidad().getEstadoCompatibilidad());
    }

    private ConfiguracionPc configuracion(UUID id, String presupuesto) {
        return ConfiguracionPc.builder().id(id).nombre("Setup").presupuesto(new BigDecimal(presupuesto))
                .moneda("PEN").estado("BORRADOR").totalEstimado(BigDecimal.ZERO).build();
    }
}
