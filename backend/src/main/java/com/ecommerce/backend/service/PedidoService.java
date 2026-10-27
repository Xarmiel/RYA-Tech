package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.PedidoDto;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String BASE = "SELECT * FROM rya.pedidos WHERE usuario_id=:usuario AND is_deleted=false";

    public PaginatedResponseDto<PedidoDto> listar(UUID usuario, int page, int size) {
        var params = new MapSqlParameterSource("usuario", usuario).addValue("limite", size)
                .addValue("offset", (long) page * size);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM rya.pedidos WHERE usuario_id=:usuario AND is_deleted=false", params, Long.class);
        var pedidos = jdbc.query(BASE + " ORDER BY created_at DESC, id DESC LIMIT :limite OFFSET :offset", params, this::cabecera);
        var content = completar(usuario, pedidos);
        int pages = (int) Math.ceil((double) total / size);
        return PaginatedResponseDto.<PedidoDto>builder().content(content).page(page).size(size)
                .totalElements(total).totalPages(pages).first(page == 0).last(page + 1 >= pages).build();
    }

    public PedidoDto detalle(UUID usuario, UUID pedidoId) {
        var params = new MapSqlParameterSource("usuario", usuario).addValue("id", pedidoId);
        var pedidos = jdbc.query(BASE + " AND id=:id", params, this::cabecera);
        if (pedidos.isEmpty()) throw new ResourceNotFoundException("Pedido no encontrado");
        return completar(usuario, pedidos).getFirst();
    }

    private PedidoDto cabecera(ResultSet rs, int row) throws SQLException {
        return new PedidoDto(rs.getObject("id", UUID.class), rs.getString("codigo"), rs.getString("estado"),
                rs.getString("moneda").trim(), rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"),
                rs.getBigDecimal("envio"), rs.getBigDecimal("monto_total"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getString("metodo_pago_snapshot"), List.of(), new PedidoDto.Entrega(rs.getString("modalidad_entrega"),
                rs.getString("direccion_envio"), rs.getString("referencia_envio"), rs.getString("entrega_snapshot"),
                rs.getString("estado_entrega"), rs.getString("transportista_seguimiento"), rs.getString("codigo_seguimiento")));
    }

    private List<PedidoDto> completar(UUID usuario, List<PedidoDto> pedidos) {
        if (pedidos.isEmpty()) return List.of();
        Map<UUID, List<PedidoDto.Item>> items = new HashMap<>();
        var params = new MapSqlParameterSource("usuario", usuario)
                .addValue("ids", pedidos.stream().map(PedidoDto::id).toList());
        // Los nombres, SKU, cantidades y precios son los registrados al comprar.
        // El catálogo actual solo aporta un enlace/imagen si el producto sigue publicado.
        jdbc.query("""
            SELECT d.*, p.slug,
              (SELECT i.url FROM rya.imagenes_producto i WHERE i.producto_id=p.id AND i.is_deleted=false
                ORDER BY i.principal DESC, i.posicion, i.id LIMIT 1) AS imagen
            FROM rya.detalle_pedido d
            JOIN rya.pedidos pe ON pe.id=d.pedido_id
            LEFT JOIN rya.productos p ON p.id=d.producto_id AND p.is_deleted=false AND p.activo=true AND p.vendible=true
            WHERE pe.usuario_id=:usuario AND pe.is_deleted=false AND d.pedido_id IN (:ids) AND d.is_deleted=false
            ORDER BY d.created_at, d.id
            """, params, (ResultSet rs) -> {
                var item = new PedidoDto.Item(rs.getObject("id", UUID.class), rs.getObject("producto_id", UUID.class),
                        rs.getString("descripcion_snapshot"), rs.getString("sku_snapshot"), rs.getInt("cantidad"),
                        rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("descuento_unitario"),
                        rs.getBigDecimal("subtotal"), rs.getString("slug"), rs.getString("imagen"));
                items.computeIfAbsent(rs.getObject("pedido_id", UUID.class), key -> new ArrayList<>()).add(item);
            });
        return pedidos.stream().map(p -> new PedidoDto(p.id(), p.codigo(), p.estado(), p.moneda(), p.subtotal(),
                p.descuento(), p.envio(), p.total(), p.fecha(), p.metodoPago(), items.getOrDefault(p.id(), List.of()), p.entrega())).toList();
    }
}
