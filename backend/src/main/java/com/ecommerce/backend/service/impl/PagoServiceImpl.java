package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.config.MercadoPagoProperties;
import com.ecommerce.backend.dto.pago.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

    private final MercadoPagoProperties mercadoPagoProperties;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final ProductoRepository productoRepository;
    private final TarifaEnvioRepository tarifaEnvioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public IniciarPagoResponseDto iniciarPago(String sessionKey, UUID usuarioId, CrearPedidoPagoRequestDto request) {
        if (usuarioId == null) {
            throw new BadRequestException("Debes iniciar sesión para proceder con el pago del pedido.");
        }

        Carrito carrito = carritoRepository.findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(
                usuarioId, "ACTIVO")
                .orElseThrow(() -> new BadRequestException("No tienes un carrito activo con productos."));

        List<ItemCarrito> items = itemCarritoRepository.findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(carrito.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("El carrito se encuentra vacío.");
        }

        TarifaEnvio tarifa = tarifaEnvioRepository.findByIdAndIsDeletedFalse(request.getEntrega().getTarifaEnvioId())
                .orElseThrow(() -> new BadRequestException("La tarifa de envío seleccionada no existe o no está activa."));

        metodoPagoRepository.findByIdAndActivoTrueAndIsDeletedFalse(request.getMetodoPagoId())
                .orElseThrow(() -> new BadRequestException("El método de pago no está disponible."));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PedidoItem> pedidoItems = new ArrayList<>();

        for (ItemCarrito item : items) {
            Producto producto = productoRepository.findConDetallesPorId(item.getProducto().getId())
                    .orElseThrow(() -> new BadRequestException("El producto " + item.getProducto().getNombre() + " ya no existe."));

            if (!Boolean.TRUE.equals(producto.getActivo()) || !Boolean.TRUE.equals(producto.getVendible())
                    || Boolean.TRUE.equals(producto.getIsDeleted())) {
                throw new BadRequestException("El producto " + producto.getNombre() + " no está disponible para venta.");
            }

            BigDecimal precioUnitario = producto.getPrecioBase();
            BigDecimal itemSubtotal = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
            subtotal = subtotal.add(itemSubtotal);

            PedidoItem pedidoItem = PedidoItem.builder()
                    .productoId(producto.getId())
                    .sku(producto.getSku())
                    .nombreProducto(producto.getNombre())
                    .precioUnitario(precioUnitario)
                    .cantidad(item.getCantidad())
                    .subtotal(itemSubtotal)
                    .build();

            pedidoItems.add(pedidoItem);
        }

        BigDecimal costoEnvio = tarifa.getCosto();
        BigDecimal total = subtotal.add(costoEnvio);

        String numeroPedido = "RYA-" + System.currentTimeMillis() % 100000000 + "-" + (100 + RANDOM.nextInt(900));

        Pedido pedido = Pedido.builder()
                .numeroPedido(numeroPedido)
                .usuarioId(usuarioId)
                .sessionKey(sessionKey)
                .estado("PENDIENTE_PAGO")
                .moneda("PEN")
                .subtotal(subtotal)
                .costoEnvio(costoEnvio)
                .total(total)
                .tipoDocumento(request.getCliente().getTipoDocumento())
                .numeroDocumento(request.getCliente().getNumeroDocumento())
                .nombreCompleto(request.getCliente().getNombre())
                .email(request.getCliente().getEmail())
                .telefono(request.getCliente().getTelefono())
                .tarifaEnvioId(tarifa.getId())
                .direccionEnvio(request.getEntrega().getDireccion())
                .referenciaEnvio(request.getEntrega().getReferencia())
                .build();

        for (PedidoItem pi : pedidoItems) {
            pi.setPedido(pedido);
        }
        pedido.setItems(pedidoItems);

        // Crear Preference en Mercado Pago
        String preferenceId = crearPreferenceMercadoPago(numeroPedido, pedidoItems, costoEnvio, request.getCliente().getEmail());
        pedido.setPreferenceId(preferenceId);
        pedidoRepository.save(pedido);

        return IniciarPagoResponseDto.builder()
                .pedidoId(pedido.getId())
                .numeroPedido(pedido.getNumeroPedido())
                .preferenceId(preferenceId)
                .total(total)
                .moneda("PEN")
                .publicKey(mercadoPagoProperties.getPublicKey())
                .build();
    }

    @Override
    @Transactional
    public ResultadoPagoDto procesarCargo(String sessionKey, UUID usuarioId, ProcesarCargoRequestDto request) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (!"PENDIENTE_PAGO".equals(pedido.getEstado())) {
            throw new BadRequestException("El pedido " + pedido.getNumeroPedido() + " ya fue procesado o cancelado.");
        }

        Map<String, Object> paymentResponse;
        boolean exito = false;
        String paymentId = null;
        String status = null;
        String statusDetail = null;
        String mensajeRespuesta = null;

        try {
            paymentResponse = ejecutarPagoMercadoPago(pedido, request);
            paymentId = String.valueOf(paymentResponse.get("id"));
            status = (String) paymentResponse.get("status");
            statusDetail = (String) paymentResponse.get("status_detail");

            exito = "approved".equalsIgnoreCase(status);
            mensajeRespuesta = switch (status != null ? status : "") {
                case "approved" -> "¡Pago aprobado con éxito!";
                case "in_process" -> "Tu pago está siendo revisado por la entidad bancaria.";
                case "rejected" -> "El pago fue rechazado. Revisa tu saldo o intenta con otro método.";
                default -> "Estado del pago: " + status;
            };
        } catch (RestClientResponseException ex) {
            log.error("Error al procesar pago en Mercado Pago: {}", ex.getResponseBodyAsString(), ex);
            status = "rejected";
            statusDetail = String.valueOf(ex.getStatusCode().value());
            mensajeRespuesta = "No pudimos procesar el pago con Mercado Pago. Verifica los datos o intenta con otro método.";
        } catch (Exception ex) {
            log.error("Fallo inesperado al conectar con Mercado Pago: {}", ex.getMessage(), ex);
            status = "error";
            mensajeRespuesta = "Error de comunicación con la pasarela de pago.";
        }

        // Registrar pago
        Pago pago = Pago.builder()
                .pedido(pedido)
                .pasarela("MERCADO_PAGO")
                .mpPaymentId(paymentId)
                .paymentMethodId(request.getPaymentMethodId())
                .monto(pedido.getTotal())
                .moneda(pedido.getMoneda())
                .estado(status != null ? status : "rejected")
                .statusDetail(statusDetail)
                .mensajeRespuesta(mensajeRespuesta)
                .build();
        pagoRepository.save(pago);

        // Si fue aprobado, actualizar pedido y limpiar carrito
        if (exito) {
            pedido.setEstado("PAGADO");
            pedidoRepository.save(pedido);

            if (pedido.getUsuarioId() != null) {
                carritoRepository.findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(pedido.getUsuarioId(), "ACTIVO")
                        .ifPresent(c -> {
                            List<ItemCarrito> items = itemCarritoRepository.findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(c.getId());
                            for (ItemCarrito ic : items) {
                                ic.setIsDeleted(true);
                            }
                            itemCarritoRepository.saveAll(items);
                        });
            }
        }

        return ResultadoPagoDto.builder()
                .exitoso(exito)
                .pedidoId(pedido.getId())
                .numeroPedido(pedido.getNumeroPedido())
                .estadoPedido(pedido.getEstado())
                .mpPaymentId(paymentId)
                .mensaje(mensajeRespuesta)
                .monto(pedido.getTotal())
                .moneda(pedido.getMoneda())
                .build();
    }

    @Override
    @Transactional
    public void procesarWebhook(String eventPayload, String signature) {
        log.info("Webhook recibido de Mercado Pago: payload={}", eventPayload);
    }

    private String crearPreferenceMercadoPago(String numeroPedido, List<PedidoItem> items, BigDecimal costoEnvio, String email) {
        if (mercadoPagoProperties.getAccessToken().startsWith("TEST-placeholder") || mercadoPagoProperties.getAccessToken().isBlank()) {
            log.warn("Usando MERCADOPAGO_ACCESS_TOKEN simulado. Generando preferenceId de prueba.");
            return "pref_test_" + System.currentTimeMillis();
        }

        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(mercadoPagoProperties.getApiUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + mercadoPagoProperties.getAccessToken())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            List<Map<String, Object>> mpItems = new ArrayList<>();
            for (PedidoItem pi : items) {
                mpItems.add(Map.of(
                        "id", pi.getSku(),
                        "title", pi.getNombreProducto(),
                        "quantity", pi.getCantidad(),
                        "unit_price", pi.getPrecioUnitario(),
                        "currency_id", "PEN"
                ));
            }
            if (costoEnvio.compareTo(BigDecimal.ZERO) > 0) {
                mpItems.add(Map.of(
                        "id", "SHIPPING",
                        "title", "Costo de Envío a Domicilio",
                        "quantity", 1,
                        "unit_price", costoEnvio,
                        "currency_id", "PEN"
                ));
            }

            Map<String, Object> body = Map.of(
                    "items", mpItems,
                    "payer", Map.of("email", email),
                    "external_reference", numeroPedido
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/checkout/preferences")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        } catch (Exception e) {
            log.error("Error al crear Preference en Mercado Pago: {}. Usando fallback local.", e.getMessage());
        }

        return "pref_test_" + System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ejecutarPagoMercadoPago(Pedido pedido, ProcesarCargoRequestDto request) {
        if (mercadoPagoProperties.getAccessToken().startsWith("TEST-placeholder") || mercadoPagoProperties.getAccessToken().isBlank()) {
            log.info("Simulando pago exitoso en Mercado Pago para pedido={}", pedido.getNumeroPedido());
            return Map.of(
                    "id", "mp_test_" + System.currentTimeMillis(),
                    "status", "approved",
                    "status_detail", "accredited"
            );
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(mercadoPagoProperties.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + mercadoPagoProperties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", pedido.getTotal());
        body.put("description", "Compra en RYA-Tech Pedido #" + pedido.getNumeroPedido());
        body.put("payment_method_id", request.getPaymentMethodId() != null ? request.getPaymentMethodId() : "visa");
        body.put("external_reference", pedido.getNumeroPedido());
        body.put("payer", Map.of("email", request.getEmail()));

        if (request.getToken() != null && !request.getToken().isBlank()) {
            body.put("token", request.getToken());
            body.put("installments", request.getInstallments() != null ? request.getInstallments() : 1);
            if (request.getIssuerId() != null && !request.getIssuerId().isBlank()) {
                body.put("issuer_id", request.getIssuerId());
            }
        }

        return restClient.post()
                .uri("/v1/payments")
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
