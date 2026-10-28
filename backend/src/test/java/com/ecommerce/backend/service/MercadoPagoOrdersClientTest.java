package com.ecommerce.backend.service;

import com.ecommerce.backend.config.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoOrdersClientTest {
    @Test void usaOrdersConAutorizacionEIdempotenciaSinPreferencesNiPayments(){
        var config=new MercadoPagoProperties();config.setPublicKey("public-test");config.setAccessToken("private-test");
        var builder=RestClient.builder().baseUrl("https://api.mercadopago.com");var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoOrdersClient(config,builder.build());
        server.expect(requestTo("https://api.mercadopago.com/v1/orders")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization","Bearer private-test")).andExpect(header("X-Idempotency-Key","attempt-1"))
            .andExpect(jsonPath("$.total_amount").value("100.00")).andRespond(withSuccess("{\"id\":\"ORD1\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/v1/orders/ORD1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        assertEquals("ORD1",client.create("attempt-1",Map.of("total_amount","100.00")).path("id").asText());client.get("ORD1");server.verify();
    }
    @Test void credencialesAusentesNoProducenAprobacionesSimuladas(){
        assertThrows(ResponseStatusException.class,()->new MercadoPagoOrdersClient(new MercadoPagoProperties()).requireCredentials());
    }
}
