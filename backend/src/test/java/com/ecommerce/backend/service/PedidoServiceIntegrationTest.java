package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.PerfilUsuarioDto;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PedidoServiceIntegrationTest {
    @Autowired PedidoService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean CuentaService cuentas;
    private final UUID user = UUID.randomUUID(), other = UUID.randomUUID();
    private UUID order, foreign, product;

    @BeforeEach
    void preparar() {
        order = pedido(user, "RYA-UNO", OffsetDateTime.now());
        foreign = pedido(other, "RYA-AJENO", OffsetDateTime.now());
        UUID category = UUID.randomUUID(), type = UUID.randomUUID(); product = UUID.randomUUID();
        jdbc.update("INSERT INTO rya.categorias (id,nombre,slug) VALUES (?,?,?)", category, "Procesadores", category.toString());
        jdbc.update("INSERT INTO rya.tipos_producto (id,codigo) VALUES (?,?)", type, "HW");
        jdbc.update("INSERT INTO rya.productos (id,categoria_id,tipo_id,sku,nombre,slug,precio_base) VALUES (?,?,?,?,?,?,?)",
                product, category, type, "SKU-ACTUAL", "Nombre actual", "producto-actual", 999);
        jdbc.update("INSERT INTO rya.detalle_pedido (id,pedido_id,producto_id,descripcion_snapshot,sku_snapshot,cantidad,precio_unitario,descuento_unitario) VALUES (?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), order, product, "Nombre al comprar", "SKU-HISTORICO", 2, 200, 5);
        when(cuentas.obtenerOSincronizar(any())).thenAnswer(invocation -> {
            org.springframework.security.oauth2.jwt.Jwt token = invocation.getArgument(0);
            return PerfilUsuarioDto.builder().id(UUID.fromString(token.getSubject())).build();
        });
    }

    @Test
    void paginaSoloPedidosPropiosOrdenadosPorFecha() {
        UUID older = pedido(user, "RYA-ANTERIOR", OffsetDateTime.now().minusDays(1));
        var first = service.listar(user, 0, 1);
        var second = service.listar(user, 1, 1);
        assertEquals(2, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(order, first.getContent().getFirst().id());
        assertEquals(older, second.getContent().getFirst().id());
    }

    @Test
    void conservaNombresEImportesHistoricosAunqueCambieOSeRetireElProducto() {
        var original = service.detalle(user, order);
        jdbc.update("UPDATE rya.productos SET precio_base=3000,is_deleted=true WHERE id=?", product);
        var result = service.detalle(user, order);
        var item = result.productos().getFirst();
        assertEquals("Nombre al comprar", item.nombre());
        assertEquals("SKU-HISTORICO", item.sku());
        assertEquals(new BigDecimal("200.00"), item.precioUnitario());
        assertEquals(new BigDecimal("390.00"), item.subtotal());
        assertEquals(new BigDecimal("395.00"), result.total());
        assertEquals(original.total(), result.total());
        assertNull(item.slug());
        assertNull(item.imagen());
    }

    @Test
    void noPermiteConsultarPedidosAjenosOEliminados() {
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(user, foreign));
        jdbc.update("UPDATE rya.pedidos SET is_deleted=true WHERE id=?", order);
        assertThrows(ResourceNotFoundException.class, () -> service.detalle(user, order));
        assertEquals(0, service.listar(user, 0, 12).getTotalElements());
    }

    @Test
    void elUsuarioProvieneDelTokenYNoDeParametrosDelCliente() throws Exception {
        mvc.perform(get("/api/v1/pedidos").param("usuarioId", other.toString())
                .with(jwt().jwt(token -> token.subject(user.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].codigo").value("RYA-UNO"));
        mvc.perform(get("/api/v1/pedidos/" + foreign).with(jwt().jwt(token -> token.subject(user.toString()))))
                .andExpect(status().isNotFound());
    }

    private UUID pedido(UUID owner, String code, OffsetDateTime fecha) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO rya.pedidos (id,usuario_id,codigo,estado,subtotal,descuento,envio,monto_total,created_at,metodo_pago_snapshot) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, owner, code, "PAGADO", 390, 10, 15, 395, fecha, "Transferencia");
        return id;
    }
}
