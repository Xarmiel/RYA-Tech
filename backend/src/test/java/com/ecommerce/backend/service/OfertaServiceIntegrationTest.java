package com.ecommerce.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OfertaServiceIntegrationTest {
    @Autowired OfertaService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void excluyePromocionesVencidasFuturasInactivasYSinRebaja() {
        crear("Vigente", "80", "100", -2, 2, true, 3);
        crear("Vencida", "80", "100", -4, -1, true, 3);
        crear("Futura", "80", "100", 1, 3, true, 3);
        crear("Inactiva", "80", "100", -2, 2, false, 3);
        crear("Sin rebaja", "100", "100", -2, 2, true, 3);
        var response = service.listar(null, null, null, false, null, "descuento", 0, 12);
        assertEquals(1, response.totalElements());
        assertEquals("Vigente", response.content().getFirst().nombre());
        assertEquals(new BigDecimal("20.00"), response.content().getFirst().descuentoPorcentaje());
        assertEquals(new BigDecimal("80.00"), response.content().getFirst().precioActual());
    }

    @Test
    void filtraAntesDePaginarYCalculaDescuentosDecimales() {
        crear("CPU A", "89.95", "100", -2, 2, true, 3);
        crear("CPU B", "50", "100", -2, 2, true, 0);
        var conStock = service.listar("CPU", null, null, true, new BigDecimal("10"), "precio-asc", 0, 1);
        assertEquals(1, conStock.totalElements());
        assertEquals(new BigDecimal("10.05"), conStock.content().getFirst().ahorro());
        assertEquals(new BigDecimal("10.05"), conStock.content().getFirst().descuentoPorcentaje());
        var second = service.listar(null, null, null, false, null, "precio-asc", 1, 1);
        assertEquals(2, second.totalPages());
        assertEquals("CPU A", second.content().getFirst().nombre());
    }

    @Test
    void noDuplicaProductosYSeleccionaUnaPromocionVigente() {
        UUID product = crear("CPU", "80", "100", -2, 2, true, 0);
        UUID promotion = UUID.randomUUID();
        jdbc.update("INSERT INTO rya.promociones (id,nombre,inicio_at,fin_at) VALUES (?,?,?,?)",
                promotion, "Termina primero", OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusHours(1));
        jdbc.update("INSERT INTO rya.promociones_productos (id,promocion_id,producto_id,precio_anterior) VALUES (?,?,?,?)",
                UUID.randomUUID(), promotion, product, 100);
        var response = service.listar(null, null, null, false, null, "fin", 0, 12);
        assertEquals(1, response.totalElements());
        assertEquals("Termina primero", response.content().getFirst().promocion());
    }

    @Test
    void busquedaEscapaComodinesYLosFiltrosSeObtienenDeOfertasReales() {
        crear("CPU% ahorro", "80", "100", -2, 2, true, 0);
        crear("CPU ahorro", "80", "100", -2, 2, true, 0);
        var response = service.listar("%", null, null, false, null, "nombre", 0, 12);
        assertEquals(1, response.totalElements());
        assertEquals("CPU% ahorro", response.content().getFirst().nombre());
        assertEquals(2, response.categorias().size());
        assertEquals(2, response.marcas().size());
        var category = response.categorias().getFirst().slug();
        assertEquals(1, service.listar(null, category, null, false, null, "descuento", 0, 12).totalElements());
    }

    private UUID crear(String nombre, String price, String previous, int start, int end, boolean active, int stock) {
        UUID category = UUID.randomUUID(), brand = UUID.randomUUID(), type = UUID.randomUUID();
        UUID product = UUID.randomUUID(), promotion = UUID.randomUUID();
        jdbc.update("INSERT INTO rya.categorias (id,nombre,slug) VALUES (?,?,?)", category, "Categoría " + category, category.toString());
        jdbc.update("INSERT INTO rya.marcas (id,nombre,slug) VALUES (?,?,?)", brand, "Marca " + brand, brand.toString());
        jdbc.update("INSERT INTO rya.tipos_producto (id,codigo) VALUES (?,?)", type, "HW");
        jdbc.update("INSERT INTO rya.productos (id,categoria_id,marca_id,tipo_id,sku,nombre,slug,precio_base) VALUES (?,?,?,?,?,?,?,?)",
                product, category, brand, type, product.toString(), nombre, product.toString(), new BigDecimal(price));
        jdbc.update("INSERT INTO rya.promociones (id,nombre,inicio_at,fin_at,activa) VALUES (?,?,?,?,?)",
                promotion, nombre, OffsetDateTime.now().plusHours(start), OffsetDateTime.now().plusHours(end), active);
        jdbc.update("INSERT INTO rya.promociones_productos (id,promocion_id,producto_id,precio_anterior) VALUES (?,?,?,?)",
                UUID.randomUUID(), promotion, product, new BigDecimal(previous));
        jdbc.update("INSERT INTO rya.existencias (id,almacen_id,producto_id,cantidad,reservado,version) VALUES (?,?,?,?,0,0)",
                UUID.randomUUID(), UUID.randomUUID(), product, stock);
        return product;
    }
}
