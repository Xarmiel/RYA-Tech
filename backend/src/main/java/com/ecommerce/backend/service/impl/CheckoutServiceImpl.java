package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.MetodoPago;
import com.ecommerce.backend.entity.Producto;
import com.ecommerce.backend.entity.TarifaEnvio;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.MetodoPagoRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.TarifaEnvioRepository;
import com.ecommerce.backend.service.CarritoService;
import com.ecommerce.backend.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutServiceImpl implements CheckoutService {
    private final CarritoService carritoService;
    private final MetodoPagoRepository metodoPagoRepository;
    private final TarifaEnvioRepository tarifaEnvioRepository;
    private final ProductoRepository productoRepository;

    @Override
    public CheckoutOpcionesDto obtenerOpciones() {
        List<CheckoutOpcionesDto.MetodoPagoDto> pagos = metodoPagoRepository
                .findByActivoTrueAndIsDeletedFalseOrderByNombreAsc().stream()
                .filter(item -> java.util.Set.of("MP_CARDS","MP_YAPE","MP_PAGOEFECTIVO").contains(item.getCodigo()))
                .map(item -> CheckoutOpcionesDto.MetodoPagoDto.builder()
                        .id(item.getId()).codigo(item.getCodigo()).nombre(item.getNombre()).build())
                .toList();
        List<CheckoutOpcionesDto.TarifaEnvioDto> tarifas = tarifaEnvioRepository.findDisponibles().stream()
                .map(item -> CheckoutOpcionesDto.TarifaEnvioDto.builder().id(item.getId())
                        .transportista(item.getTransportista()).departamento(item.getDepartamento())
                        .provincia(item.getProvincia()).distrito(item.getDistrito()).costo(item.getCosto())
                        .plazoDias(item.getPlazoDias()).build())
                .toList();
        return CheckoutOpcionesDto.builder().metodosPago(pagos).tarifasEnvio(tarifas).build();
    }

    @Override
    public CheckoutValidacionDto validar(String sessionKey, UUID usuarioId, ValidarCheckoutRequestDto request) {
        validarDocumento(request.getCliente());
        CarritoDto carrito = carritoService.obtener(sessionKey, usuarioId);
        if (carrito.getItems().isEmpty()) throw new BadRequestException("El carrito está vacío");

        List<String> mensajes = new ArrayList<>();
        for (ItemCarritoDto item : carrito.getItems()) {
            Producto producto = productoRepository.findConDetallesPorId(item.getProductoId())
                    .orElseThrow(() -> new BadRequestException("Un producto del carrito ya no está disponible"));
            if (!Boolean.TRUE.equals(producto.getActivo()) || !Boolean.TRUE.equals(producto.getVendible())
                    || Boolean.TRUE.equals(producto.getIsDeleted()) || producto.getPrecioBase() == null) {
                throw new BadRequestException(item.getNombre() + " ya no está disponible para la venta");
            }
            if (item.getStockDisponible() < item.getCantidad()) {
                mensajes.add("Stock insuficiente para " + item.getNombre() + ". Disponible: " + item.getStockDisponible());
            }
            if (Boolean.TRUE.equals(item.getPrecioActualizado())) {
                mensajes.add("El precio de " + item.getNombre() + " fue actualizado.");
            }
        }
        if (mensajes.stream().anyMatch(message -> message.startsWith("Stock insuficiente"))) {
            throw new BadRequestException(String.join(" ", mensajes));
        }

        MetodoPago metodo = metodoPagoRepository.findByIdAndActivoTrueAndIsDeletedFalse(request.getMetodoPagoId())
                .orElseThrow(() -> new BadRequestException("El método de pago ya no está disponible"));
        TarifaEnvio tarifa = tarifaEnvioRepository.findByIdAndIsDeletedFalse(request.getEntrega().getTarifaEnvioId())
                .orElseThrow(() -> new BadRequestException("La tarifa de envío ya no está disponible"));
        BigDecimal envio = tarifa.getCosto();
        BigDecimal total = carrito.getSubtotal().add(envio);
        String destino = Stream.of(tarifa.getDepartamento(), tarifa.getProvincia(), tarifa.getDistrito())
                .filter(value -> value != null && !value.isBlank()).reduce((left, right) -> left + ", " + right)
                .orElse("Cobertura general");
        mensajes.add("Precios, stock, envío y método de pago fueron revalidados.");
        boolean requiereAutenticacion = usuarioId == null;
        if (requiereAutenticacion) {
            mensajes.add("Debes iniciar sesión antes de crear el pedido y continuar con la pasarela de pago.");
        }
        return CheckoutValidacionDto.builder().valido(true).requiereAutenticacion(requiereAutenticacion).moneda(carrito.getMoneda())
                .subtotal(carrito.getSubtotal()).envio(envio).total(total).metodoPago(metodo.getNombre())
                .entrega(destino).mensajes(mensajes).carrito(carrito).build();
    }

    private void validarDocumento(ValidarCheckoutRequestDto.Cliente cliente) {
        String numero = cliente.getNumeroDocumento();
        boolean valido = switch (cliente.getTipoDocumento()) {
            case "DNI" -> numero.matches("\\d{8}");
            case "RUC" -> numero.matches("\\d{11}");
            case "CE" -> numero.matches("[A-Za-z0-9]{6,12}");
            default -> false;
        };
        if (!valido) throw new BadRequestException("El número de documento no coincide con el tipo seleccionado");
    }
}
