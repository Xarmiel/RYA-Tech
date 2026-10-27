package com.ecommerce.backend.service;
import com.ecommerce.backend.dto.TarifaEnvioRequest;
import com.ecommerce.backend.dto.ValidarCheckoutRequestDto;
import com.ecommerce.backend.entity.Pedido;
import com.ecommerce.backend.repository.PedidoRepository;
import com.ecommerce.backend.repository.TarifaEnvioRepository;
import com.ecommerce.backend.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class EnvioIntegrationTest {
 @Autowired EnvioAdminService admin;
 @Autowired TarifaEnvioRepository tarifas;
 @Autowired PedidoRepository pedidos;
 @Autowired PedidoService compras;
 private TarifaEnvioRequest tarifa(String mode,boolean active){return new TarifaEnvioRequest("Servicio prueba",mode,"Lima",null,null,new BigDecimal(mode.equals("RECOJO")?"0.00":"12.00"),2,mode.equals("RECOJO")?"Av. Prueba 123":null,mode.equals("RECOJO")?"L-V 9 a 18":null,active);}
 @Test void guardaEditaDesactivaYNoOfreceTarifasInactivas(){
   var rate=admin.guardar(null,tarifa("DOMICILIO",true));tarifas.flush();
   assertTrue(tarifas.findDisponibles().stream().anyMatch(r->r.getId().equals(rate.getId())&&r.getModalidad().equals("DOMICILIO")));
   admin.guardar(rate.getId(),tarifa("DOMICILIO",false));tarifas.flush();
   assertTrue(tarifas.findByIdAndIsDeletedFalse(rate.getId()).isEmpty());
   assertFalse(tarifas.findDisponibles().stream().anyMatch(r->r.getId().equals(rate.getId())));
   assertTrue(admin.listar().stream().anyMatch(r->r.getId().equals(rate.getId())));
 }
 @Test void compruebaCoberturaEnServidorYRecojoSinDireccionDelCliente(){
   var rate=admin.guardar(null,tarifa("DOMICILIO",true));var delivery=new ValidarCheckoutRequestDto.Entrega();
   delivery.setDepartamento("LIMA");delivery.setProvincia("Lima");delivery.setDistrito("San Miguel");delivery.setDireccion("Av. Cliente 456");
   assertDoesNotThrow(()->EntregaRules.validar(rate,delivery));delivery.setDepartamento("Cusco");
   assertThrows(BadRequestException.class,()->EntregaRules.validar(rate,delivery));
   var pickup=admin.guardar(null,tarifa("RECOJO",true));assertDoesNotThrow(()->EntregaRules.validar(pickup,new ValidarCheckoutRequestDto.Entrega()));
   assertTrue(EntregaRules.descripcion(pickup,delivery).contains("L-V 9 a 18"));
   assertThrows(BadRequestException.class,()->admin.guardar(null,new TarifaEnvioRequest("Incompleto","RECOJO",null,null,null,BigDecimal.ZERO,0,null,null,true)));
 }
 private Pedido pedido(String mode,String state){return pedidos.saveAndFlush(Pedido.builder().numeroPedido("SHIP-"+UUID.randomUUID().toString().substring(0,20)).usuarioId(UUID.randomUUID()).estado(state).subtotal(BigDecimal.TEN).total(BigDecimal.TEN).tipoDocumento("DNI").numeroDocumento("12345678").nombreCompleto("Cliente prueba").email("test@example.test").telefono("999999999").direccionEnvio("Dirección original").modalidadEntrega(mode).estadoEntrega("PENDIENTE").entregaSnapshot("Horario original y tarifa original").build());}
 @Test void seguimientoExigePagoEtapasYCodigoYExponeLaCopiaHistorica(){
   var pending=pedido("DOMICILIO","PENDIENTE_PAGO");
   assertThrows(BadRequestException.class,()->admin.seguimiento(pending.getId(),"PREPARANDO",null,null,timestamp(pending.getId())));
   var paid=pedido("DOMICILIO","PAGADO");
   assertThrows(BadRequestException.class,()->admin.seguimiento(paid.getId(),"ENTREGADO","Courier","CODE",timestamp(paid.getId())));
   admin.seguimiento(paid.getId(),"PREPARANDO",null,null,timestamp(paid.getId()));
   var detail=compras.detalle(paid.getUsuarioId(),paid.getId());assertEquals("PREPARANDO",detail.entrega().estado());assertEquals("Dirección original",detail.entrega().direccion());
   java.time.OffsetDateTime current=timestamp(paid.getId());
   assertThrows(BadRequestException.class,()->admin.seguimiento(paid.getId(),"ENVIADO",null,null,current));
   admin.seguimiento(paid.getId(),"ENVIADO","Courier","ABC123",current);
   assertEquals("ABC123",compras.detalle(paid.getUsuarioId(),paid.getId()).entrega().codigoSeguimiento());
   assertThrows(ResponseStatusException.class,()->admin.seguimiento(paid.getId(),"ENTREGADO","Courier","ABC123",current.minusDays(1)));
 }
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 private java.time.OffsetDateTime timestamp(UUID id){return jdbc.queryForObject("SELECT updated_at FROM rya.pedidos WHERE id=?",java.time.OffsetDateTime.class,id);}
 @Test void recojoTieneEtapasPropias(){var order=pedido("RECOJO","PAGADO");admin.seguimiento(order.getId(),"PREPARANDO",null,null,timestamp(order.getId()));admin.seguimiento(order.getId(),"LISTO_RECOJO",null,null,timestamp(order.getId()));admin.seguimiento(order.getId(),"RETIRADO",null,null,timestamp(order.getId()));assertEquals("RETIRADO",compras.detalle(order.getUsuarioId(),order.getId()).entrega().estado());}
}
