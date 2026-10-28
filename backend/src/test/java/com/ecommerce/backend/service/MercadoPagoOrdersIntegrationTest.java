package com.ecommerce.backend.service;

import com.ecommerce.backend.config.MercadoPagoProperties;
import com.ecommerce.backend.dto.pago.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class MercadoPagoOrdersIntegrationTest {
    @Autowired PagoService service;
    @Autowired ClientePagoRepository customerBindings;
    @Autowired PedidoRepository pedidos;
    @Autowired PagoRepository pagos;
    @Autowired ObjectMapper mapper;
    @Autowired MercadoPagoProperties config;
    @Autowired ProductoService products;
    @Autowired ProductoRepository productRepo;
    @Autowired CategoriaRepository categories;
    @Autowired TipoProductoRepository types;
    @Autowired PedidoService purchases;
    @MockitoBean MetodoPagoRepository methods;
    @MockitoBean TarifaEnvioRepository shipping;
    @MockitoBean MercadoPagoOrdersClient client;
    @MockitoBean CarritoRepository carts;
    @MockitoBean ItemCarritoRepository items;
    @MockitoBean CheckoutService checkout;
    @BeforeEach void setup(){reset(client,carts,items);config.setLiveMode(false);config.setWebhookSecret("test-webhook-secret");}
    Pedido pedido(String method){return pedidos.saveAndFlush(Pedido.builder().numeroPedido("ORD-"+UUID.randomUUID().toString().substring(0,20))
        .usuarioId(UUID.randomUUID()).estado("PENDIENTE_PAGO").moneda("PEN").subtotal(new BigDecimal("95.00")).costoEnvio(new BigDecimal("5.00")).total(new BigDecimal("100.00"))
        .tipoDocumento("DNI").numeroDocumento("12345678").nombreCompleto("Cliente de prueba").email("buyer@example.test").telefono("999999999")
        .direccionEnvio("Dirección de prueba").metodoPagoCodigo(method).build());}
    ProcesarCargoRequestDto request(Pedido p){return ProcesarCargoRequestDto.builder().pedidoId(p.getId()).email("buyer@example.test")
        .token("token-no-persistir").paymentMethodId("visa").paymentTypeId("credit_card").installments(1).build();}
    JsonNode response(String reference,String status){
        var order=mapper.createObjectNode().put("id","ORD"+reference.replace("-","" )).put("external_reference",reference).put("type","online")
            .put("currency_id","PEN").put("live_mode",false).put("total_amount","100.00").put("status",status)
            .put("status_detail",status.equals("processed")?"accredited":"waiting_payment");
        var payment=order.putObject("transactions").putArray("payments").addObject().put("id","PAY"+reference.replace("-",""))
            .put("amount","100.00").put("status",status).put("status_detail",status.equals("processed")?"accredited":"waiting_payment");
        payment.putObject("payment_method").put("id","visa").put("type","credit_card");return order;
    }
    String signature(String id,String requestId)throws Exception{
        var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(config.getWebhookSecret().getBytes(UTF_8),"HmacSHA256"));
        String ts="1234567890";return "ts="+ts+",v1="+HexFormat.of().formatHex(mac.doFinal(("id:"+id.toLowerCase(Locale.ROOT)+";request-id:"+requestId+";ts:"+ts+";").getBytes(UTF_8)));
    }
    @Test void iniciarUsaCarritoAbiertoRevalidaYSeVeEnMisComprasSinCrearPreference(){
        UUID user=UUID.randomUUID();var cart=Carrito.builder().id(UUID.randomUUID()).usuarioId(user).estado("ABIERTO").build();
        when(carts.findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(user,"ABIERTO")).thenReturn(Optional.of(cart));
        var category=categories.save(com.ecommerce.backend.entity.Categoria.builder().nombre("Mouse").slug("orders-"+UUID.randomUUID()).build());
        var type=types.save(com.ecommerce.backend.entity.TipoProducto.builder().codigo("T"+UUID.randomUUID().toString().substring(0,8)).descripcion("Prueba").build());
        var product=products.crearProducto(com.ecommerce.backend.dto.AdminProductoDto.builder().nombre("Mouse de prueba").sku("ORDER-"+UUID.randomUUID()).categoriaId(category.getId()).tipoId(type.getId()).precioBase(new BigDecimal("95.00")).stockInicial(30).estado("active").build());
        when(items.findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(cart.getId())).thenReturn(List.of(ItemCarrito.builder().producto(productRepo.findById(product.getId()).orElseThrow()).cantidad(1).build()));
        var method=mock(MetodoPago.class);when(method.getCodigo()).thenReturn("MP_CARDS");when(method.getNombre()).thenReturn("Tarjeta");
        var rate=mock(TarifaEnvio.class);UUID rateId=UUID.randomUUID();when(rate.getActivo()).thenReturn(true);when(rate.getModalidad()).thenReturn("DOMICILIO");when(rate.getNombreServicio()).thenReturn("Courier de prueba");when(rate.getId()).thenReturn(rateId);when(rate.getCosto()).thenReturn(new BigDecimal("5.00"));
        UUID methodId=UUID.randomUUID();when(methods.findByIdAndActivoTrueAndIsDeletedFalse(methodId)).thenReturn(Optional.of(method));when(shipping.findByIdAndIsDeletedFalse(rateId)).thenReturn(Optional.of(rate));
        var customer=new com.ecommerce.backend.dto.ValidarCheckoutRequestDto.Cliente();customer.setNombre("Cliente prueba");customer.setEmail("buyer@example.test");customer.setTipoDocumento("DNI");customer.setNumeroDocumento("12345678");customer.setTelefono("999999999");
        var delivery=new com.ecommerce.backend.dto.ValidarCheckoutRequestDto.Entrega();delivery.setDepartamento("Lima");delivery.setProvincia("Lima");delivery.setDistrito("Miraflores");delivery.setTarifaEnvioId(rateId);delivery.setDireccion("Dirección de prueba");
        var response=service.iniciarPago("session",user,CrearPedidoPagoRequestDto.builder().cliente(customer).entrega(delivery).metodoPagoId(methodId).build());
        var saved=pedidos.findById(response.getPedidoId()).orElseThrow();
        assertEquals("PENDIENTE",saved.getEstado());assertEquals("DOMICILIO", saved.getModalidadEntrega());assertTrue(saved.getEntregaSnapshot().contains("Courier de prueba"));
        when(rate.getNombreServicio()).thenReturn("Nombre posterior");assertTrue(pedidos.findById(saved.getId()).orElseThrow().getEntregaSnapshot().contains("Courier de prueba"));
        assertEquals(0,new BigDecimal("100.00").compareTo(response.getTotal()));assertEquals("MP_CARDS",response.getMetodoPagoCodigo());
        assertEquals(1,purchases.detalle(user,response.getPedidoId()).productos().size());assertEquals(response.getNumeroPedido(),purchases.detalle(user,response.getPedidoId()).codigo());
        verify(client,never()).create(anyString(),anyMap());verify(checkout).validar(eq("session"),eq(user),any());
    }
    @Test void pedidoPendienteDeLaBaseActualPuedePagarse(){
        var p=pedido("MP_CARDS");p.setEstado("PENDIENTE");pedidos.saveAndFlush(p);
        when(client.create(anyString(),anyMap())).thenAnswer(call->response(call.getArgument(0),"processed"));
        assertTrue(service.procesarCargo("",p.getUsuarioId(),request(p)).isExitoso());assertEquals("PAGADO",pedidos.findById(p.getId()).orElseThrow().getEstado());
    }
    @Test void creaOrderConImporteDelServidorYNoRepiteUnCobroAprobado(){
        var p=pedido("MP_CARDS");when(client.create(anyString(),anyMap())).thenAnswer(call->{
            var body=(Map<?,?>)call.getArgument(1);assertEquals("100.00",body.get("total_amount"));assertEquals("automatic",body.get("processing_mode"));
            assertEquals(call.getArgument(0),body.get("external_reference"));assertFalse(body.containsKey("transaction_amount"));return response(call.getArgument(0),"processed");});
        var paid=service.procesarCargo("",p.getUsuarioId(),request(p));assertTrue(paid.isExitoso());assertFalse(paid.isPendiente());assertNotNull(paid.getMpOrderId());
        assertEquals("PAGADO",pedidos.findById(p.getId()).orElseThrow().getEstado());
        when(client.get(paid.getMpOrderId())).thenReturn(response(pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId()).getFirst().getId().toString(),"processed"));
        service.procesarCargo("",p.getUsuarioId(),request(p));verify(client,times(1)).create(anyString(),anyMap());
        var stored=pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId());assertEquals(1,stored.size());assertNull(stored.getFirst().getRawResponse());
    }
    @Test void tarjetaGuardadaSeVerificaEnLaCuentaAntesDeCrearLaOrder()throws Exception{
        var p=pedido("MP_CARDS");var binding=new com.ecommerce.backend.entity.ClientePago();binding.setId(UUID.randomUUID());binding.setUsuarioId(p.getUsuarioId());
        var scope="test:"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(config.getPublicKey().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        binding.setEntorno(scope);binding.setCustomerId("owned-customer");customerBindings.saveAndFlush(binding);
        when(client.cards("owned-customer")).thenReturn(mapper.readTree("[{\"id\":\"saved1\",\"last_four_digits\":\"9876\",\"expiration_month\":12,\"expiration_year\":2035,\"payment_method\":{\"id\":\"visa\",\"name\":\"Visa\",\"payment_type_id\":\"debit_card\"}}]"));
        var req=request(p);req.setSavedCardId("saved1");req.setPaymentMethodId("tampered");req.setPaymentTypeId("credit_card");
        when(client.create(anyString(),anyMap())).thenAnswer(call->{var body=(Map<?,?>)call.getArgument(1);assertEquals("owned-customer",((Map<?,?>)body.get("payer")).get("customer_id"));var payments=(java.util.List<?>)((Map<?,?>)body.get("transactions")).get("payments");var method=(Map<?,?>)((Map<?,?>)payments.getFirst()).get("payment_method");assertEquals("visa",method.get("id"));assertEquals("debit_card",method.get("type"));return response(call.getArgument(0),"processed");});
        assertTrue(service.procesarCargo("",p.getUsuarioId(),req).isExitoso());
    }
    @Test void pedidoAjenoNoPuedeSerPagadoNiConsultado(){
        var p=pedido("MP_CARDS");assertThrows(ResourceNotFoundException.class,()->service.procesarCargo("",UUID.randomUUID(),request(p)));
        assertThrows(ResourceNotFoundException.class,()->service.consultarPago(p.getId(),UUID.randomUUID()));verify(client,never()).create(anyString(),anyMap());
    }
    @Test void timeoutQuedaPendienteSinGenerarOtroIntentoAlRepetir(){
        var p=pedido("MP_CARDS");when(client.create(anyString(),anyMap())).thenThrow(new ResourceAccessException("timeout"));
        assertTrue(service.procesarCargo("",p.getUsuarioId(),request(p)).isPendiente());service.procesarCargo("",p.getUsuarioId(),request(p));
        assertEquals("PENDIENTE_PAGO",pedidos.findById(p.getId()).orElseThrow().getEstado());assertEquals("unknown",pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId()).getFirst().getEstado());
        verify(client,times(1)).create(anyString(),anyMap());
    }
    @Test void importeMonedaEntornoYReferenciaDebenCoincidirAntesDeAprobar(){
        for(String field:List.of("total_amount","currency_id","live_mode","external_reference")){
            reset(client);var p=pedido("MP_CARDS");when(client.create(anyString(),anyMap())).thenAnswer(call->{var json=(com.fasterxml.jackson.databind.node.ObjectNode)response(call.getArgument(0),"processed");
                if(field.equals("live_mode"))json.put(field,true);else json.put(field,field.equals("total_amount")?"1.00":field.equals("currency_id")?"USD":UUID.randomUUID().toString());return json;});
            var result=service.procesarCargo("",p.getUsuarioId(),request(p));assertFalse(result.isExitoso());assertTrue(result.isPendiente());assertEquals("PENDIENTE_PAGO",pedidos.findById(p.getId()).orElseThrow().getEstado());
        }
    }
    @Test void pagoEfectivoDevuelveInstruccionesSinMarcarPedidoPagado(){
        var p=pedido("MP_PAGOEFECTIVO");var request=request(p);request.setPaymentMethodId("pagoefectivo_atm");request.setToken(null);
        when(client.create(anyString(),anyMap())).thenAnswer(call->{var order=response(call.getArgument(0),"action_required");
            ((com.fasterxml.jackson.databind.node.ObjectNode)order.path("transactions").path("payments").get(0).path("payment_method"))
                .put("id","pagoefectivo_atm").put("type","atm").put("reference","12345678").put("ticket_url","https://www.mercadopago.com.pe/payments/1/ticket");return order;});
        var result=service.procesarCargo("",p.getUsuarioId(),request);assertFalse(result.isExitoso());assertTrue(result.isPendiente());assertEquals("12345678",result.getReferenciaPago());assertNotNull(result.getInstruccionesUrl());
        verifyNoInteractions(items);
    }
    @Test void webhookSeVerificaConsultaOrderYProcesaRepeticionesSinRepetirEfectos()throws Exception{
        var p=pedido("MP_YAPE");var request=request(p);request.setPaymentMethodId("yape");when(client.create(anyString(),anyMap())).thenAnswer(call->response(call.getArgument(0),"processing"));
        var pending=service.procesarCargo("",p.getUsuarioId(),request);String orderId=pending.getMpOrderId();
        var attempt=pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId()).getFirst();when(client.get(orderId)).thenReturn(response(attempt.getId().toString(),"processing"));
        service.procesarWebhook("{\"type\":\"order\",\"data\":{\"id\":\""+orderId+"\",\"status\":\"processed\"}}",signature(orderId,"req"),"req",orderId);
        assertEquals("PENDIENTE_PAGO",pedidos.findById(p.getId()).orElseThrow().getEstado());
        when(client.get(orderId)).thenReturn(response(attempt.getId().toString(),"processed"));
        String payload="{\"type\":\"order\",\"data\":{\"id\":\""+orderId+"\",\"status\":\"failed\"}}";
        for(int i=0;i<2;i++)service.procesarWebhook(payload,signature(orderId,"req"),"req",orderId);
        assertEquals("PAGADO",pedidos.findById(p.getId()).orElseThrow().getEstado());assertEquals(1,pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId()).size());
        verify(carts,times(1)).findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(p.getUsuarioId(),"ABIERTO");
        assertThrows(ResponseStatusException.class,()->service.procesarWebhook(payload,"ts=1,v1="+"0".repeat(64),"req",orderId));
    }
    @Test void dosSolicitudesConcurrentesSoloCreanUnaOrder()throws Exception{
        var p=pedido("MP_CARDS");var reached=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(client.create(anyString(),anyMap())).thenAnswer(call->{reached.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return response(call.getArgument(0),"processed");});
        var executor=Executors.newFixedThreadPool(2);
        try{var first=executor.submit(()->service.procesarCargo("",p.getUsuarioId(),request(p)));assertTrue(reached.await(5,TimeUnit.SECONDS));
            var second=executor.submit(()->service.procesarCargo("",p.getUsuarioId(),request(p))).get(3,TimeUnit.SECONDS);assertTrue(second.isPendiente());release.countDown();assertTrue(first.get(5,TimeUnit.SECONDS).isExitoso());
            verify(client,times(1)).create(anyString(),anyMap());
        }finally{release.countDown();executor.shutdownNow();}
    }
    @Test void respuestaJsonSePersisteComoJsonYNoComoTexto()throws Exception{
        var order=pedido("MP_CARDS");
        var payment=pagos.saveAndFlush(Pago.builder().pedido(order).monto(order.getTotal()).estado("processing").rawResponse("{\"status\":\"processing\"}").build());
        assertEquals("processing",mapper.readTree(pagos.findById(payment.getId()).orElseThrow().getRawResponse()).path("status").asText());
        verify(client,never()).create(anyString(),anyMap());
    }
    @Test void consultarPedidoSinIntentosIndicaQueElPagoNoSeInicio(){
        var order=pedido("MP_CARDS");var result=service.consultarPago(order.getId(),order.getUsuarioId());
        assertEquals("not_started",result.getEstadoPago());assertFalse(result.isPendiente());assertFalse(result.isExitoso());
        verify(client,never()).create(anyString(),anyMap());verify(client,never()).get(anyString());
    }
    @Test void reconciliaRespuestaRealDeOrdersSinRepetirElCobro()throws Exception{
        var p=pedido("MP_CARDS");when(client.create(anyString(),anyMap())).thenThrow(new ResourceAccessException("timeout"));
        service.procesarCargo("",p.getUsuarioId(),request(p));
        var attempt=pagos.findByPedidoIdOrderByCreatedAtDesc(p.getId()).getFirst();
        var actual=(com.fasterxml.jackson.databind.node.ObjectNode)response(attempt.getId().toString(),"processed");
        actual.remove(List.of("currency_id","live_mode"));actual.put("currency","PEN").put("id","ORDTSTverified");
        when(client.search(eq(attempt.getId()),any())).thenReturn(mapper.createObjectNode().set("data",mapper.createArrayNode().add(actual)));
        when(client.get("ORDTSTverified")).thenReturn(actual);
        assertTrue(service.consultarPago(p.getId(),p.getUsuarioId()).isExitoso());
        assertEquals("PAGADO",pedidos.findById(p.getId()).orElseThrow().getEstado());
        verify(client,times(1)).create(anyString(),anyMap());verify(client).get("ORDTSTverified");
    }
}
