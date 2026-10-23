package com.ecommerce.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RYA Tech API - Base Esencial")
                        .version("1.0.0")
                        .description("API REST para el catálogo, filtros dinámicos, asesoría/PC Builder y comparador de CPU de RYA Tech basada en PostgreSQL (esquema rya).")
                        .contact(new Contact()
                                .name("RYA Tech Development Team")
                                .email("soporte@ryatech.pe"))
                        .license(new License().name("Privada - RYA Tech")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Servidor Local de Desarrollo")
                ));
    }
}
