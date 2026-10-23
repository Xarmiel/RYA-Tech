package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AgregarItemCarritoRequestDto;
import com.ecommerce.backend.dto.CarritoDto;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.impl.CarritoServiceImpl;
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
class CarritoServiceTest {
    @Mock CarritoRepository carritoRepository;
    @Mock ItemCarritoRepository itemRepository;
    @Mock ProductoRepository productoRepository;
    @Mock ExistenciaRepository existenciaRepository;
    @Mock ImagenProductoRepository imagenProductoRepository;

    private CarritoServiceImpl service;
    private final String session = "sesion-anonima-prueba-123456789";

    @BeforeEach
    void setUp() {
        service = new CarritoServiceImpl(carritoRepository, itemRepository, productoRepository,
                existenciaRepository, imagenProductoRepository);
    }

    @Test
    void carritoInexistenteSeDevuelveVacioSinEscribir() {
        when(carritoRepository.findFirstBySessionKeyHashAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(any(), eq("ABIERTO")))
                .thenReturn(Optional.empty());

        CarritoDto result = service.obtener(session, null);

        assertNull(result.getId());
        assertEquals(0, result.getCantidadTotal());
        assertTrue(result.getItems().isEmpty());
        verify(carritoRepository, never()).save(any());
    }

    @Test
    void agregaProductoConPrecioYStockVigentes() {
        UUID productId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Producto product = Producto.builder().id(productId).sku("CPU-1").slug("cpu-1").nombre("CPU")
                .precioBase(new BigDecimal("450.00")).activo(true).vendible(true).isDeleted(false).build();
        Carrito cart = Carrito.builder().id(cartId).sessionKeyHash("hash").estado("ABIERTO").moneda("PEN").build();

        when(productoRepository.findConDetallesPorId(productId)).thenReturn(Optional.of(product));
        when(carritoRepository.findFirstBySessionKeyHashAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(any(), eq("ABIERTO")))
                .thenReturn(Optional.of(cart));
        when(itemRepository.findByCarritoIdAndProductoIdAndIsDeletedFalse(cartId, productId)).thenReturn(Optional.empty());
        when(existenciaRepository.findStockDisponiblePorProductoId(productId)).thenReturn(4);
        when(itemRepository.save(any())).thenAnswer(invocation -> {
            ItemCarrito item = invocation.getArgument(0); item.setId(UUID.randomUUID()); return item;
        });
        when(itemRepository.findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(cartId)).thenAnswer(invocation -> {
            ItemCarrito item = ItemCarrito.builder().id(UUID.randomUUID()).carrito(cart).producto(product)
                    .cantidad(2).precioReferencia(product.getPrecioBase()).build();
            return List.of(item);
        });

        CarritoDto result = service.agregar(session, null,
                AgregarItemCarritoRequestDto.builder().productoId(productId).cantidad(2).build());

        assertEquals(2, result.getCantidadTotal());
        assertEquals(0, result.getSubtotal().compareTo(new BigDecimal("900.00")));
        verify(itemRepository).save(argThat(item -> item.getPrecioReferencia().compareTo(new BigDecimal("450.00")) == 0));
    }

    @Test
    void rechazaCantidadSuperiorAlStock() {
        UUID productId = UUID.randomUUID();
        Producto product = Producto.builder().id(productId).nombre("CPU").precioBase(BigDecimal.TEN)
                .activo(true).vendible(true).isDeleted(false).build();
        Carrito cart = Carrito.builder().id(UUID.randomUUID()).estado("ABIERTO").build();
        when(productoRepository.findConDetallesPorId(productId)).thenReturn(Optional.of(product));
        when(carritoRepository.findFirstBySessionKeyHashAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(any(), eq("ABIERTO")))
                .thenReturn(Optional.of(cart));
        when(itemRepository.findByCarritoIdAndProductoIdAndIsDeletedFalse(cart.getId(), productId)).thenReturn(Optional.empty());
        when(existenciaRepository.findStockDisponiblePorProductoId(productId)).thenReturn(1);

        assertThrows(BadRequestException.class, () -> service.agregar(session, null,
                AgregarItemCarritoRequestDto.builder().productoId(productId).cantidad(2).build()));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void adoptaCarritoAnonimoAlAutenticarSinPerderlo() {
        UUID usuarioId = UUID.randomUUID();
        Carrito anonimo = Carrito.builder().id(UUID.randomUUID()).sessionKeyHash("hash")
                .estado("ABIERTO").moneda("PEN").build();
        when(carritoRepository.findFirstBySessionKeyHashAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(any(), eq("ABIERTO")))
                .thenReturn(Optional.of(anonimo));
        when(carritoRepository.findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(usuarioId, "ABIERTO"))
                .thenReturn(Optional.empty());
        when(carritoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(anonimo.getId())).thenReturn(List.of());

        CarritoDto result = service.vincular(session, usuarioId);

        assertEquals(anonimo.getId(), result.getId());
        assertEquals(usuarioId, anonimo.getUsuarioId());
        assertNull(anonimo.getSessionKeyHash());
        verify(carritoRepository).save(anonimo);
    }
}
