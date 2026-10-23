package com.ecommerce.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;
    @Test void healthIsPublicAndDoesNotExposeConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/productos")
            .header("Origin","http://localhost:4200").header("Access-Control-Request-Method","GET"))
            .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/productos")
            .header("Origin","https://untrusted.example").header("Access-Control-Request-Method","GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminAccessRequiresAuthenticationAndTrustedRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/acceso")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/acceso").with(jwt()
                .jwt(token -> token.claim("user_metadata", Map.of("role", "admin")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(false));
        mockMvc.perform(post("/api/v1/admin/productos").with(jwt()
                .jwt(token -> token.claim("user_metadata", Map.of("role", "admin")))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/acceso").with(jwt()
                .jwt(token -> token.claim("app_metadata", Map.of("role", "admin")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    void favoritosRequierenTokenJwt() throws Exception {
        mockMvc.perform(get("/api/v1/favoritos")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/favoritos/sincronizar")
                .contentType("application/json").content("{\"agregar\":[],\"eliminar\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comprasRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pedidos/11111111-1111-4111-8111-111111111111"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilRequiereTokenJwt() throws Exception {
        mockMvc.perform(get("/api/v1/cuenta/perfil"))
                .andExpect(status().isUnauthorized());
    }
}
