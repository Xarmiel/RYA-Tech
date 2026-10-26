package com.ecommerce.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {
    private String publicKey = "TEST-placeholder";
    private String accessToken = "TEST-placeholder";
    private String webhookSecret = "";
    private String apiUrl = "https://api.mercadopago.com";
}
