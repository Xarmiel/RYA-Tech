package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ActualizarRolRequestDto;
import com.ecommerce.backend.dto.AdminUsuarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usertest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles("test")
@Transactional
class AdminUsuarioServiceIntegrationTest {

    @Autowired
    private AdminUsuarioService usuarioService;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID user1;
    private UUID user2;
    private Jwt adminActor;

    @BeforeEach
    void setUp() {
        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        adminActor = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(actorId.toString())
                .claim("app_metadata", Map.of("role", "admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        jdbc.update("INSERT INTO auth.users(id, email, raw_app_meta_data, raw_user_meta_data) VALUES(?, ?, ?::json, ?::json)",
                user1, "testuser1@gmail.com", "{\"role\":\"user\"}", "{\"nombre\":\"Test Uno\",\"telefono\":\"999111222\"}");

        jdbc.update("INSERT INTO auth.users(id, email, raw_app_meta_data, raw_user_meta_data) VALUES(?, ?, ?::json, ?::json)",
                user2, "testadmin@gmail.com", "{\"role\":\"admin\"}", "{\"nombre\":\"Test Admin\",\"telefono\":\"999333444\"}");

        jdbc.update("INSERT INTO rya.usuarios(id, nombre, email, telefono, estado) VALUES(?, ?, ?, ?, ?)",
                user1, "Test Uno", "testuser1@gmail.com", "999111222", "ACTIVO");
    }

    @Test
    @DisplayName("listarUsuarios debe retornar los usuarios registrados")
    void testListarUsuarios() {
        PaginatedResponseDto<AdminUsuarioDto> resultado = usuarioService.listarUsuarios(null, null, 0, 10);
        assertNotNull(resultado);
        assertTrue(resultado.getTotalElements() >= 2);
        assertTrue(resultado.getContent().stream().anyMatch(u -> "testuser1@gmail.com".equals(u.getEmail())));
        assertTrue(resultado.getContent().stream().anyMatch(u -> "testadmin@gmail.com".equals(u.getEmail())));
    }

    @Test
    @DisplayName("cambiarRol debe cambiar de 'user' a 'admin'")
    void testCambiarRol() {
        AdminUsuarioDto updated = usuarioService.cambiarRol(user1, new ActualizarRolRequestDto("admin"), adminActor);
        assertNotNull(updated);
        assertEquals("admin", updated.getRol());
        assertEquals(user1, updated.getId());

        // Verificar cambio a 'user'
        AdminUsuarioDto demoted = usuarioService.cambiarRol(user1, new ActualizarRolRequestDto("user"), adminActor);
        assertNotNull(demoted);
        assertEquals("user", demoted.getRol());
    }
}
