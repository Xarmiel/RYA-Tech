package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.SincronizarFavoritosRequestDto;
import com.ecommerce.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritoService {
    private final JdbcTemplate jdbc;
    private final CuentaService cuentas;

    @Transactional
    public List<UUID> listar(Jwt jwt) {
        UUID usuario = cuentas.obtenerOSincronizar(jwt).getId();
        return ids(usuario);
    }

    @Transactional
    public List<UUID> sincronizar(Jwt jwt, SincronizarFavoritosRequestDto request) {
        UUID usuario = cuentas.obtenerOSincronizar(jwt).getId();
        if (request.agregar().stream().anyMatch(request.eliminar()::contains)) {
            throw new BadRequestException("Un producto no puede agregarse y eliminarse al mismo tiempo");
        }
        for (UUID producto : request.eliminar()) {
            jdbc.update("""
                    UPDATE rya.favoritos SET is_deleted = true, deleted_at = now(),
                        deleted_by = ?, updated_by = ?
                    WHERE usuario_id = ? AND producto_id = ? AND is_deleted = false
                    """, usuario, usuario, usuario, producto);
        }
        // INSERT SELECT impide guardar IDs inventados o productos retirados.
        // La restricción única hace idempotentes las altas y reactiva favoritos eliminados.
        for (UUID producto : request.agregar()) {
            jdbc.update("""
                    INSERT INTO rya.favoritos (usuario_id, producto_id, created_by, updated_by)
                    SELECT ?, p.id, ?, ? FROM rya.productos p
                    WHERE p.id = ? AND p.is_deleted = false AND p.activo = true AND p.vendible = true
                    ON CONFLICT (usuario_id, producto_id) DO UPDATE
                    SET is_deleted = false, deleted_at = NULL, deleted_by = NULL,
                        updated_at = now(), updated_by = EXCLUDED.updated_by
                    """, usuario, usuario, usuario, producto);
        }
        return ids(usuario);
    }

    private List<UUID> ids(UUID usuario) {
        return jdbc.query("""
                SELECT producto_id FROM rya.favoritos
                WHERE usuario_id = ? AND is_deleted = false ORDER BY created_at DESC, producto_id
                """, (rs, row) -> rs.getObject("producto_id", UUID.class), usuario);
    }
}
