package com.ecommerce.backend.service;

import com.ecommerce.backend.config.MercadoPagoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Component
public class MercadoPagoOrdersClient {
    private final MercadoPagoProperties config;
    private final RestClient client;
    @Autowired public MercadoPagoOrdersClient(MercadoPagoProperties config) {this(config,build(config));}
    public MercadoPagoOrdersClient(MercadoPagoProperties config,RestClient client) {this.config=config;this.client=client;}
    private static RestClient build(MercadoPagoProperties config) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl(config.getApiUrl()).requestFactory(factory).build();
    }
    public void requireCredentials() {
        if(config.getAccessToken()==null||config.getAccessToken().isBlank()||config.getAccessToken().contains("placeholder")
            ||config.getPublicKey()==null||config.getPublicKey().isBlank()||config.getPublicKey().contains("placeholder"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Falta configurar las credenciales de Mercado Pago. No se simulan cobros ni aprobaciones.");
    }
    public JsonNode create(String key,Map<String,Object> body) {
        requireCredentials();return client.post().uri("/v1/orders").headers(h->h.setBearerAuth(config.getAccessToken()))
            .header("X-Idempotency-Key",key).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
    }
    public JsonNode get(String id) {
        requireCredentials();if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("Identificador de order inválido");
        return client.get().uri("/v1/orders/{id}",id).headers(h->h.setBearerAuth(config.getAccessToken())).retrieve().body(JsonNode.class);
    }
}
