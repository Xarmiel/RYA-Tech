package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.MetodoPago;
import com.ecommerce.backend.entity.Producto;
import com.ecommerce.backend.entity.TarifaEnvio;
import com.ecommerce.backend.repository.MetodoPagoRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.TarifaEnvioRepository;
import com.ecommerce.backend.service.impl.CheckoutServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {
    @Test
    void revalidaCarritoYCalculaTotalConTarifaReal() {
        CarritoService carritoService = mock(CarritoService.class);
        MetodoPagoRepository pagos = mock(MetodoPagoRepository.class);
        TarifaEnvioRepository tarifas = mock(TarifaEnvioRepository.class);
        ProductoRepository productos = mock(ProductoRepository.class);
        UUID pagoId = UUID.randomUUID(), tarifaId = UUID.randomUUID(), productoId = UUID.randomUUID();
        MetodoPago pago = mock(MetodoPago.class);
        TarifaEnvio tarifa = mock(TarifaEnvio.class);
        Producto producto = Producto.builder().activo(true).vendible(true).isDeleted(false)
                .precioBase(new BigDecimal("100.00")).build();
        when(pago.getNombre()).thenReturn("Pasarela segura");
        when(tarifa.getActivo()).thenReturn(true);
        when(tarifa.getCosto()).thenReturn(new BigDecimal("15.00"));
        when(tarifa.getDepartamento()).thenReturn("Lima");
        when(carritoService.obtener("session-key-valid-123", null))
                .thenReturn(CarritoDto.builder().moneda("PEN").subtotal(new BigDecimal("100.00"))
                        .items(List.of(ItemCarritoDto.builder().productoId(productoId).nombre("CPU").cantidad(1).stockDisponible(2)
                                .precioActualizado(false).build())).build());
        when(productos.findConDetallesPorId(productoId)).thenReturn(Optional.of(producto));
        when(pagos.findByIdAndActivoTrueAndIsDeletedFalse(pagoId)).thenReturn(Optional.of(pago));
        when(tarifas.findByIdAndIsDeletedFalse(tarifaId)).thenReturn(Optional.of(tarifa));

        ValidarCheckoutRequestDto request = request(pagoId, tarifaId);
        CheckoutValidacionDto result = new CheckoutServiceImpl(carritoService, pagos, tarifas, productos)
                .validar("session-key-valid-123", null, request);

        assertTrue(result.isValido());
        assertTrue(result.isRequiereAutenticacion());
        assertEquals("115.00", result.getTotal().toPlainString());
    }

    private ValidarCheckoutRequestDto request(UUID pagoId, UUID tarifaId) {
        ValidarCheckoutRequestDto dto = new ValidarCheckoutRequestDto();
        ValidarCheckoutRequestDto.Cliente cliente = new ValidarCheckoutRequestDto.Cliente();
        cliente.setTipoDocumento("DNI"); cliente.setNumeroDocumento("12345678");
        cliente.setNombre("Cliente Real"); cliente.setEmail("cliente@example.com"); cliente.setTelefono("999999999");
        ValidarCheckoutRequestDto.Entrega entrega = new ValidarCheckoutRequestDto.Entrega();
        entrega.setDepartamento("Lima"); entrega.setProvincia("Lima"); entrega.setDistrito("Miraflores"); entrega.setTarifaEnvioId(tarifaId); entrega.setDireccion("Dirección válida");
        dto.setCliente(cliente); dto.setEntrega(entrega); dto.setMetodoPagoId(pagoId);
        return dto;
    }
}
