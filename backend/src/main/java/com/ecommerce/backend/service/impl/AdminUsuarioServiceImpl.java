package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.ActualizarRolRequestDto;
import com.ecommerce.backend.dto.AdminUsuarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.service.AdminUsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUsuarioServiceImpl implements AdminUsuarioService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<AdminUsuarioDto> listarUsuarios(String busqueda, String rol, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("El número de página no puede ser menor a 0");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("El tamaño de página debe estar entre 1 y 100");
        }

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (busqueda != null && !busqueda.isBlank()) {
            whereClause.append(" AND (LOWER(u.email) LIKE :busqueda ")
                    .append(" OR LOWER(COALESCE(ru.nombre, '')) LIKE :busqueda ")
                    .append(" OR COALESCE(ru.telefono, '') LIKE :busqueda) ");
            params.addValue("busqueda", "%" + busqueda.trim().toLowerCase() + "%");
        }

        String countSql = "SELECT COUNT(*) FROM auth.users u LEFT JOIN rya.usuarios ru ON ru.id = u.id " + whereClause;
        Long totalElements = jdbc.queryForObject(countSql, params, Long.class);
        if (totalElements == null) {
            totalElements = 0L;
        }

        int offset = page * size;
        params.addValue("limit", size);
        params.addValue("offset", offset);

        String selectSql = """
            SELECT
                u.id,
                u.email,
                u.created_at,
                u.raw_app_meta_data,
                u.raw_user_meta_data,
                ru.nombre AS rya_nombre,
                ru.telefono AS rya_telefono,
                ru.estado AS rya_estado,
                (SELECT COUNT(*) FROM rya.pedidos p WHERE p.usuario_id = u.id AND p.is_deleted = false) AS total_pedidos,
                (SELECT MAX(p.created_at) FROM rya.pedidos p WHERE p.usuario_id = u.id AND p.is_deleted = false) AS ultima_compra
            FROM auth.users u
            LEFT JOIN rya.usuarios ru ON ru.id = u.id
            """ + whereClause + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";

        List<AdminUsuarioDto> usuarios = jdbc.query(selectSql, params, (rs, rowNum) -> mapRow(rs));

        // Filtrado por rol en memoria si se especificó el filtro
        if (rol != null && !rol.isBlank()) {
            String rolTarget = rol.trim().toLowerCase();
            usuarios = usuarios.stream()
                    .filter(u -> "admin".equals(rolTarget) ? "admin".equalsIgnoreCase(u.getRol()) : !"admin".equalsIgnoreCase(u.getRol()))
                    .toList();
        }

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return PaginatedResponseDto.<AdminUsuarioDto>builder()
                .content(usuarios)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    @Override
    @Transactional
    public AdminUsuarioDto cambiarRol(UUID usuarioId, ActualizarRolRequestDto request, Jwt jwtActor) {
        String nuevoRol = request.getRol().trim().toLowerCase();
        if (!"admin".equals(nuevoRol) && !"user".equals(nuevoRol)) {
            throw new BadRequestException("El rol debe ser 'admin' o 'user'");
        }

        if (jwtActor != null && usuarioId.toString().equalsIgnoreCase(jwtActor.getSubject()) && !"admin".equals(nuevoRol)) {
            throw new BadRequestException("No puedes revocar tu propio rol de administrador desde tu sesión activa");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("id", usuarioId);
        String currentMeta = null;
        try {
            Object metaObj = jdbc.queryForObject("SELECT raw_app_meta_data FROM auth.users WHERE id = :id", params, Object.class);
            if (metaObj instanceof byte[] bytes) {
                currentMeta = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } else if (metaObj != null) {
                currentMeta = metaObj.toString();
            }
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("No se encontró la cuenta de usuario especificada");
        }

        ObjectNode metaNode;
        try {
            if (currentMeta == null || currentMeta.isBlank()) {
                metaNode = objectMapper.createObjectNode();
            } else {
                JsonNode parsed = objectMapper.readTree(currentMeta);
                if (parsed.isTextual()) {
                    parsed = objectMapper.readTree(parsed.asText());
                }
                metaNode = parsed.isObject() ? (ObjectNode) parsed : objectMapper.createObjectNode();
            }
            metaNode.put("role", nuevoRol);
        } catch (Exception e) {
            log.error("Error al procesar raw_app_meta_data para usuario {}", usuarioId, e);
            throw new BadRequestException("No se pudo procesar los metadatos de la cuenta");
        }

        params.addValue("meta", metaNode.toString());
        jdbc.update("UPDATE auth.users SET raw_app_meta_data = cast(:meta as jsonb), updated_at = now() WHERE id = :id", params);

        log.info("Rol de usuario {} actualizado a '{}' por administrador {}", usuarioId, nuevoRol,
                jwtActor != null ? jwtActor.getSubject() : "sistema");

        return obtenerUsuarioPorId(usuarioId);
    }

    private AdminUsuarioDto obtenerUsuarioPorId(UUID id) {
        String sql = """
            SELECT
                u.id,
                u.email,
                u.created_at,
                u.raw_app_meta_data,
                u.raw_user_meta_data,
                ru.nombre AS rya_nombre,
                ru.telefono AS rya_telefono,
                ru.estado AS rya_estado,
                (SELECT COUNT(*) FROM rya.pedidos p WHERE p.usuario_id = u.id AND p.is_deleted = false) AS total_pedidos,
                (SELECT MAX(p.created_at) FROM rya.pedidos p WHERE p.usuario_id = u.id AND p.is_deleted = false) AS ultima_compra
            FROM auth.users u
            LEFT JOIN rya.usuarios ru ON ru.id = u.id
            WHERE u.id = :id
            """;
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource("id", id), (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }
    }

    private AdminUsuarioDto mapRow(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String email = rs.getString("email");
        String ryaNombre = rs.getString("rya_nombre");
        String ryaTelefono = rs.getString("rya_telefono");
        String estado = rs.getString("rya_estado");
        if (estado == null || estado.isBlank()) {
            estado = "ACTIVO";
        }

        String rawUserMeta = obtenerJsonString(rs, "raw_user_meta_data");
        String rawAppMeta = obtenerJsonString(rs, "raw_app_meta_data");

        String nombre = ryaNombre;
        if (nombre == null || nombre.isBlank()) {
            nombre = extraerJsonCampo(rawUserMeta, "nombre");
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Usuario";
        }

        String telefono = ryaTelefono;
        if (telefono == null || telefono.isBlank()) {
            telefono = extraerJsonCampo(rawUserMeta, "telefono");
        }

        String rol = extraerJsonCampo(rawAppMeta, "role");
        if (rol == null || !"admin".equalsIgnoreCase(rol)) {
            rol = "user";
        } else {
            rol = "admin";
        }

        long totalPedidos = rs.getLong("total_pedidos");
        Timestamp ultimaCompraTs = rs.getTimestamp("ultima_compra");
        OffsetDateTime ultimaCompra = ultimaCompraTs != null
                ? ultimaCompraTs.toInstant().atOffset(ZoneOffset.UTC)
                : null;

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        OffsetDateTime createdAt = createdAtTs != null
                ? createdAtTs.toInstant().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        return AdminUsuarioDto.builder()
                .id(id)
                .nombre(nombre)
                .email(email)
                .telefono(telefono)
                .rol(rol)
                .estado(estado)
                .totalPedidos(totalPedidos)
                .ultimaCompra(ultimaCompra)
                .createdAt(createdAt)
                .build();
    }

    private String obtenerJsonString(ResultSet rs, String column) {
        try {
            Object obj = rs.getObject(column);
            if (obj == null) return null;
            if (obj instanceof byte[] bytes) {
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerJsonCampo(String jsonStr, String campo) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            if (node != null && node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (node == null || !node.isObject()) return null;
            JsonNode val = node.get(campo);
            return val != null && !val.isNull() ? val.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
