package com.ecommerce.backend.service;
import com.ecommerce.backend.config.MercadoPagoProperties;
import com.ecommerce.backend.entity.ClientePago;
import com.ecommerce.backend.repository.ClientePagoRepository;
import com.ecommerce.backend.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc
class TarjetasPagoIntegrationTest {
 @Autowired TarjetasPagoService service;@Autowired ClientePagoRepository repository;@Autowired MercadoPagoProperties config;@Autowired ObjectMapper mapper;@Autowired MockMvc mvc;
 @MockitoBean MercadoPagoOrdersClient client;
 final UUID user=UUID.randomUUID();
 String card="""
 {"id":"card1","last_four_digits":"1234","first_six_digits":"411111","expiration_month":12,"expiration_year":2035,"security_code":{"length":3},"payment_method":{"id":"visa","name":"Visa","payment_type_id":"debit_card"},"live_mode":false}
 """;
 @BeforeEach void setup()throws Exception{config.setPublicKey("public-wallet-test");config.setLiveMode(false);when(client.customerCreate(anyString())).thenReturn(mapper.readTree("{\"id\":\"customer1\"}"));when(client.saveCard(anyString(),anyString())).thenReturn(mapper.readTree(card));when(client.cards(anyString())).thenReturn(mapper.readTree("["+card+"]"));}
 @AfterEach void cleanup(){repository.deleteAll();}
 @Test void consentimientoYCuentaControlanElGuardadoSinPersistirDatosDeTarjeta(){
  assertThrows(BadRequestException.class,()->service.save(user,"owner@test.test","token",false));verify(client,never()).saveCard(anyString(),anyString());
  var result=service.save(user,"owner@test.test","single-use-token",true);assertEquals("1234",result.lastFour());
  assertEquals(1,repository.count());assertEquals("customer1",repository.findAll().getFirst().getCustomerId());
  verify(client).customerCreate("owner@test.test");assertEquals(1,service.list(user).size());assertTrue(service.list(UUID.randomUUID()).isEmpty());
  assertThrows(ResourceNotFoundException.class,()->service.owned(UUID.randomUUID(),"card1"));assertThrows(ResourceNotFoundException.class,()->service.owned(user,"card-other"));
  String json=new ObjectMapper().valueToTree(result).toString();assertFalse(json.contains("411111"));assertFalse(json.contains("single-use-token"));assertFalse(json.contains("customer1"));
 }
 @Test void clientesSeSeparanPorCuentaYEntorno(){
  service.save(user,"owner@test.test","token",true);config.setLiveMode(true);assertTrue(service.list(user).isEmpty());config.setLiveMode(false);config.setPublicKey("different-merchant");assertTrue(service.list(user).isEmpty());
 }
 @Test void falloAlAsociarNoPierdeLaRelacionDelCliente()throws Exception{
  when(client.saveCard(anyString(),anyString())).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.save(user,"owner@test.test","token",true));assertEquals("customer1",repository.findAll().getFirst().getCustomerId());
  doReturn(mapper.readTree(card)).when(client).saveCard(anyString(),anyString());service.save(user,"owner@test.test","new-token",true);verify(client,times(1)).customerCreate(anyString());
 }
 @Test void noAceptaTarjetasVencidasNiDeOtroEntorno()throws Exception{
  service.save(user,"owner@test.test","token",true);
  when(client.cards(anyString())).thenReturn(mapper.readTree("["+card.replace("2035","2020")+"]"));assertThrows(BadRequestException.class,()->service.owned(user,"card1"));
  when(client.cards(anyString())).thenReturn(mapper.readTree("["+card.replace("false","true")+"]"));assertTrue(service.list(user).isEmpty());
 }
 @Test void apiRequiereSesionYUsaCorreoFirmadoNoElDelCheckout()throws Exception{
  mvc.perform(get("/api/v1/pagos/tarjetas")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/pagos/tarjetas").with(jwt().jwt(j->j.subject(user.toString()).claim("email","verified@test.test"))).contentType("application/json").content("{\"token\":\"token\",\"consentimiento\":true,\"email\":\"attacker@test.test\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.lastFour").value("1234")).andExpect(jsonPath("$.first_six_digits").doesNotExist());
  verify(client).customerCreate("verified@test.test");
 }
}
