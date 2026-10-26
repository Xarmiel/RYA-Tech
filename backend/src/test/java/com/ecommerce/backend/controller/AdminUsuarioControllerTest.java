package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ActualizarRolRequestDto;
import com.ecommerce.backend.dto.AdminUsuarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.service.AdminUsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminUsuarioControllerTest {

    private AdminUsuarioService usuarioService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        usuarioService = mock(AdminUsuarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminUsuarioController(usuarioService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/admin/usuarios debe listar cuentas paginadas")
    void testListarUsuarios() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUsuarioDto dto = AdminUsuarioDto.builder()
                .id(id)
                .nombre("Administrador General")
                .email("admin@gmail.com")
                .telefono("987654321")
                .rol("admin")
                .estado("ACTIVO")
                .totalPedidos(0)
                .createdAt(OffsetDateTime.now())
                .build();

        PaginatedResponseDto<AdminUsuarioDto> page = PaginatedResponseDto.<AdminUsuarioDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(usuarioService.listarUsuarios(any(), any(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("admin@gmail.com"))
                .andExpect(jsonPath("$.content[0].rol").value("admin"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/usuarios/{id}/rol debe cambiar el rol del usuario")
    void testCambiarRol() throws Exception {
        UUID actorId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(actorId.toString())
                .claim("app_metadata", Map.of("role", "admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        UUID targetId = UUID.randomUUID();
        ActualizarRolRequestDto req = new ActualizarRolRequestDto("admin");
        AdminUsuarioDto updated = AdminUsuarioDto.builder()
                .id(targetId)
                .nombre("Juan Perez")
                .email("juan@gmail.com")
                .rol("admin")
                .estado("ACTIVO")
                .build();

        when(usuarioService.cambiarRol(eq(targetId), any(ActualizarRolRequestDto.class), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/usuarios/" + targetId + "/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@gmail.com"))
                .andExpect(jsonPath("$.rol").value("admin"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/usuarios/{id}/rol rechaza rol inválido")
    void testCambiarRolInvalido() throws Exception {
        UUID id = UUID.randomUUID();
        String json = "{\"rol\":\"superman\"}";

        mockMvc.perform(patch("/api/v1/admin/usuarios/" + id + "/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
