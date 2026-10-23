package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductoCatalogIntegrationTest {

    private static final UUID CATEGORIA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MARCA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TIPO_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCTO_CON_STOCK = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCTO_SIN_STOCK = UUID.fromString("40000000-0000-0000-0000-000000000002");

    @Autowired
    private ProductoService productoService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararCatalogo() {
        jdbc.update("DELETE FROM rya.existencias");
        jdbc.update("DELETE FROM rya.imagenes_producto");
        jdbc.update("DELETE FROM rya.productos");
        jdbc.update("DELETE FROM rya.marcas");
        jdbc.update("DELETE FROM rya.categorias");
        jdbc.update("DELETE FROM rya.tipos_producto");

        jdbc.update("INSERT INTO rya.categorias (id,nombre,slug,acronimo,posicion,is_deleted) VALUES (?,?,?,?,?,false)",
                CATEGORIA_ID, "Procesadores", "procesadores", "CPU", 1);
        jdbc.update("INSERT INTO rya.marcas (id,nombre,slug,is_deleted) VALUES (?,?,?,false)",
                MARCA_ID, "AMD", "amd");
        jdbc.update("INSERT INTO rya.tipos_producto (id,codigo,is_deleted) VALUES (?,?,false)",
                TIPO_ID, "HARDWARE");

        insertarProducto(PRODUCTO_CON_STOCK, "CPU-UNO", "CPU con stock", "cpu-con-stock");
        insertarProducto(PRODUCTO_SIN_STOCK, "CPU-DOS", "CPU sin stock", "cpu-sin-stock");

        jdbc.update("INSERT INTO rya.existencias (id,almacen_id,producto_id,cantidad,reservado,version,is_deleted) VALUES (?,?,?,?,?,?,false)",
                UUID.randomUUID(), UUID.randomUUID(), PRODUCTO_CON_STOCK, 8, 2, 0);
        jdbc.update("INSERT INTO rya.existencias (id,almacen_id,producto_id,cantidad,reservado,version,is_deleted) VALUES (?,?,?,?,?,?,false)",
                UUID.randomUUID(), UUID.randomUUID(), PRODUCTO_SIN_STOCK, 2, 2, 0);
    }

    @Test
    @DisplayName("El filtro de stock debe conservar totales y paginación correctos")
    void filtraStockAntesDePaginar() {
        PaginatedResponseDto<ProductoResumenDto> resultado = productoService.listarProductos(
                null, null, "procesadores", null, "amd",
                null, null, true, Map.of(), 0, 12, "nombre", "asc"
        );

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());
        assertEquals(PRODUCTO_CON_STOCK, resultado.getContent().getFirst().getId());
        assertEquals(6, resultado.getContent().getFirst().getStockDisponible());
    }

    private void insertarProducto(UUID id, String sku, String nombre, String slug) {
        jdbc.update("""
                INSERT INTO rya.productos
                (id,categoria_id,marca_id,tipo_id,sku,nombre,slug,descripcion,precio_base,costo_referencia,
                 activo,vendible,requiere_serie,requiere_envio,is_deleted)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,false)
                """,
                id, CATEGORIA_ID, MARCA_ID, TIPO_ID, sku, nombre, slug, "Ficha real", 1000, 700,
                true, true, false, true);
    }

    @Test
    void ordenaDisponibilidadAntesDePaginarYConservaTotales() {
        // El producto sin stock queda primero alfabéticamente; el orden de disponibilidad debe prevalecer.
        jdbc.update("UPDATE rya.productos SET nombre = 'AAA sin stock' WHERE id = ?", PRODUCTO_SIN_STOCK);
        var primera = productoService.listarProductos(null, null, null, null, null,
                null, null, false, Map.of(), 0, 1, "disponibilidad", "desc");
        var segunda = productoService.listarProductos(null, null, null, null, null,
                null, null, false, Map.of(), 1, 1, "disponibilidad", "desc");
        assertEquals(2, primera.getTotalElements());
        assertEquals(2, primera.getTotalPages());
        assertEquals(PRODUCTO_CON_STOCK, primera.getContent().getFirst().getId());
        assertEquals(PRODUCTO_SIN_STOCK, segunda.getContent().getFirst().getId());
    }

    @Test
    void ordenaPrecioYNombresEnAmbasDirecciones() {
        jdbc.update("UPDATE rya.productos SET precio_base = 500 WHERE id = ?", PRODUCTO_SIN_STOCK);
        var menor = productoService.listarProductos(null, null, null, null, null,
                null, null, false, Map.of(), 0, 1, "precio", "asc");
        var mayor = productoService.listarProductos(null, null, null, null, null,
                null, null, false, Map.of(), 0, 1, "precio", "desc");
        var nombreDesc = productoService.listarProductos(null, null, null, null, null,
                null, null, false, Map.of(), 0, 1, "nombre", "desc");
        assertEquals(PRODUCTO_SIN_STOCK, menor.getContent().getFirst().getId());
        assertEquals(PRODUCTO_CON_STOCK, mayor.getContent().getFirst().getId());
        assertEquals(PRODUCTO_SIN_STOCK, nombreDesc.getContent().getFirst().getId());
    }

    @Test
    @DisplayName("Detalle público no debe exponer costo interno y detalle admin sí debe incluirlo")
    void testDetallePublicoNoExponeCostoReferencia() {
        var detallePublico = productoService.obtenerPorId(PRODUCTO_CON_STOCK);
        assertEquals("CPU con stock", detallePublico.getNombre());
        // Comprobar por reflexión que ProductoDetalleDto carece del campo costoReferencia
        boolean tieneCostoReferencia = java.util.Arrays.stream(detallePublico.getClass().getDeclaredFields())
                .anyMatch(f -> "costoReferencia".equals(f.getName()));
        org.junit.jupiter.api.Assertions.assertFalse(tieneCostoReferencia,
                "El DTO público ProductoDetalleDto NUNCA debe contener el campo costoReferencia");

        // El endpoint administrativo sí debe exponer el costo de referencia autorizado
        var detalleAdmin = productoService.obtenerPorIdAdmin(PRODUCTO_CON_STOCK);
        assertEquals("CPU con stock", detalleAdmin.getNombre());
        assertEquals(0, new java.math.BigDecimal("700.00").compareTo(detalleAdmin.getCostoReferencia()),
                "El detalle administrativo debe exponer el costo de referencia real");
    }

    @Test
    @DisplayName("Filtro de alertas en admin debe retornar solo productos con stock <= 3 paginados correctamente en BD")
    void testListarProductosAdmin_FiltroAlertas() {
        var alertas = productoService.listarProductosAdmin(null, null, null, "alert", 0, 10, "nombre", "asc");
        assertEquals(1, alertas.getTotalElements(), "Solo debe haber 1 producto en alerta (stock <= 3)");
        assertEquals(1, alertas.getContent().size());
        assertEquals(PRODUCTO_SIN_STOCK, alertas.getContent().getFirst().getId());
        assertEquals(0, alertas.getContent().getFirst().getStockDisponible());
    }
}
