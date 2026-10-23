package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.PerfilUsuarioDto;
import com.ecommerce.backend.dto.SincronizarFavoritosRequestDto;
import com.ecommerce.backend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock CuentaService cuentas;
    private FavoritoService service;
    private final UUID usuario = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256")
            .subject(UUID.randomUUID().toString()).build();

    @BeforeEach
    void preparar() {
        service = new FavoritoService(jdbc, cuentas);
        when(cuentas.obtenerOSincronizar(jwt)).thenReturn(PerfilUsuarioDto.builder().id(usuario).build());
    }

    @Test
    void eliminaSolamenteFavoritosDelUsuarioAutenticado() {
        service.sincronizar(jwt, new SincronizarFavoritosRequestDto(List.of(), List.of(producto)));
        verify(jdbc).update(contains("WHERE usuario_id = ? AND producto_id = ?"),
                eq(usuario), eq(usuario), eq(usuario), eq(producto));
    }

    @Test
    void altaIdempotenteVerificaProductoRealActivoYVendible() {
        service.sincronizar(jwt, new SincronizarFavoritosRequestDto(List.of(producto), List.of()));
        verify(jdbc).update(argThat(sql -> sql.contains("p.activo = true AND p.vendible = true")
                        && sql.contains("ON CONFLICT (usuario_id, producto_id)")),
                eq(usuario), eq(usuario), eq(usuario), eq(producto));
    }

    @Test
    void rechazaCambiosContradictoriosSinModificarFavoritos() {
        assertThrows(BadRequestException.class, () -> service.sincronizar(jwt,
                new SincronizarFavoritosRequestDto(List.of(producto), List.of(producto))));
        verifyNoInteractions(jdbc);
    }
}
