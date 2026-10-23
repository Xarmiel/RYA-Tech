package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AjusteStockRequestDto;
import com.ecommerce.backend.entity.MovimientoInventario;
import com.ecommerce.backend.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:inventoryaudit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class InventarioIntegrationTest {
    @Autowired InventarioService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @org.springframework.boot.test.mock.mockito.SpyBean MovimientoInventarioRepository movements;
    private static final String ACTOR="10000000-1000-4000-8000-000000000001";
    private UUID product, warehouse, stock, category, type;

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test")
            .header("alg","RS256").subject(ACTOR).claim("app_metadata",Map.of("role","admin")).build()));
    }
    @BeforeEach void setup() {
        authenticate();
        product=UUID.randomUUID(); warehouse=UUID.randomUUID(); stock=UUID.randomUUID(); category=UUID.randomUUID(); type=UUID.randomUUID();
        jdbc.update("INSERT INTO rya.categorias(id,nombre,slug) VALUES(?,?,?)",category,"Test category",category.toString());
        jdbc.update("INSERT INTO rya.tipos_producto(id,codigo) VALUES(?,?)",type,"INV-"+type.toString().substring(0,8));
        jdbc.update("INSERT INTO rya.productos(id,categoria_id,tipo_id,sku,nombre,slug,precio_base) VALUES(?,?,?,?,?,?,?)",product,category,type,product.toString(),"Inventory test",product.toString(),10);
        jdbc.update("INSERT INTO rya.almacenes(id,nombre,codigo) VALUES(?,?,?)",warehouse,"Test warehouse",warehouse.toString());
        jdbc.update("INSERT INTO rya.existencias(id,producto_id,almacen_id,cantidad,reservado) VALUES(?,?,?,?,?)",stock,product,warehouse,10,2);
    }
    @AfterEach void clearSecurity(){ SecurityContextHolder.clearContext(); }
    private AjusteStockRequestDto request(String kind,int quantity) {
        return AjusteStockRequestDto.builder().productoId(product).almacenId(warehouse).existenciaId(stock)
            .operacionId(UUID.randomUUID()).versionEsperada(0).tipo(kind).cantidad(quantity).nota("Verified physical count").build();
    }

    @Test void recordsDirectionActorAndReplaysWithoutChangingStockAgain() {
        var req=request("AJUSTE",8);
        var result=service.registrarAjuste(req);
        assertEquals(8,result.getCantidad()); assertEquals(1,result.getVersion());
        var history=service.obtenerKardex(stock,0,20);
        assertEquals(1,history.getTotalElements());
        var event=history.getContent().get(0);
        assertEquals(-2,event.getDelta()); assertEquals(10,event.getCantidadAnterior()); assertEquals(8,event.getCantidadNueva());
        assertEquals(ACTOR,event.getActor()); assertEquals(req.getNota(),event.getNota());
        assertEquals(8,service.registrarAjuste(req).getCantidad());
        assertEquals(1,service.obtenerKardex(stock,0,20).getTotalElements());
        req.setCantidad(9);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.registrarAjuste(req)).getStatusCode().value());
    }

    @Test void staleVersionAndMismatchedExistenceAreRejected() {
        var req=request("ENTRADA",1);req.setVersionEsperada(5);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.registrarAjuste(req)).getStatusCode().value());
        req.setVersionEsperada(0);req.setExistenciaId(UUID.randomUUID());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.registrarAjuste(req)).getStatusCode().value());
        assertEquals(0,service.obtenerKardex(stock,0,20).getTotalElements());
    }

    @Test void consultsExactWarehouseAndVariantAndAllowsFirstBaseEntry() {
        UUID second=UUID.randomUUID(), variant=UUID.randomUUID(), variantStock=UUID.randomUUID();
        jdbc.update("INSERT INTO rya.almacenes(id,nombre,codigo) VALUES(?,?,?)",second,"Other",second.toString());
        jdbc.update("INSERT INTO rya.existencias(id,producto_id,almacen_id,variante_id,cantidad) VALUES(?,?,?,?,?)",variantStock,product,warehouse,variant,7);
        assertEquals(10,service.consultarStock(product,warehouse,null).getCantidad());
        assertEquals(7,service.consultarStock(product,warehouse,variant).getCantidad());
        assertEquals(-1,service.consultarStock(product,second,null).getVersion());
        var req=request("ENTRADA",4);req.setAlmacenId(second);req.setExistenciaId(null);req.setVersionEsperada(-1);
        var created=service.registrarAjuste(req);
        assertEquals(4,created.getCantidad());assertEquals("Other",created.getAlmacenNombre());
        assertEquals(10,service.consultarStock(product,warehouse,null).getCantidad());
    }

    @Test void preservesTotalsBeyondLastPageAndBoundsHistory() {
        var page=service.listarInventario(product.toString(),null,null,5,1);
        assertTrue(page.getContent().isEmpty());assertEquals(1,page.getTotalElements());assertFalse(page.isFirst());
        assertThrows(RuntimeException.class,()->service.listarInventario(null,null,null,0,101));
        for(int i=0;i<3;i++) movements.saveAndFlush(MovimientoInventario.builder().existencia(
            jdbcStock()).tipo("ENTRADA").cantidad(1).nota("legacy").build());
        assertEquals(1,service.obtenerKardex(stock,0,1).getContent().size());
        assertEquals(3,service.obtenerKardex(stock,0,1).getTotalPages());
        assertNull(service.obtenerKardex(stock,0,1).getContent().get(0).getCantidadAnterior());
    }
    @Autowired com.ecommerce.backend.repository.ExistenciaRepository stocks;
    private com.ecommerce.backend.entity.Existencia jdbcStock(){return stocks.findById(stock).orElseThrow();}

    @Test void filtersActuallyApplyAndDuplicateStockFailsSafely() {
        assertEquals(0,service.listarInventario(product.toString(),null,"alert",0,10).getTotalElements());
        jdbc.update("INSERT INTO rya.existencias(id,producto_id,almacen_id,cantidad) VALUES(?,?,?,?)",UUID.randomUUID(),product,warehouse,2);
        assertEquals(1,service.listarInventario(product.toString(),null,"alert",0,10).getTotalElements());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.consultarStock(product,warehouse,null)).getStatusCode().value());
    }

    @Test void rejectsOverflowAndInactiveWarehouseWithoutWritingHistory() {
        assertThrows(RuntimeException.class,()->service.registrarAjuste(request("ENTRADA",Integer.MAX_VALUE)));
        assertEquals(0,service.obtenerKardex(stock,0,20).getTotalElements());
        jdbc.update("UPDATE rya.almacenes SET activo=false WHERE id=?",warehouse);
        entityManager.clear();
        assertThrows(RuntimeException.class,()->service.registrarAjuste(request("ENTRADA",1)));
    }

    @Test void securityAndStrictIntegerJsonUseRealHttpFilters() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/admin/inventario")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/inventario").with(jwt())).andExpect(status().isForbidden());
        var admin=jwt().jwt(j->j.subject(ACTOR).claim("app_metadata",Map.of("role","admin")));
        mvc.perform(get("/api/v1/admin/inventario/stock").with(admin).param("productoId",product.toString()).param("almacenId",warehouse.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(10));
        mvc.perform(post("/api/v1/admin/inventario/ajuste").with(admin).contentType("application/json")
            .content("{\"cantidad\":1.8}"))
            .andExpect(status().isBadRequest());
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentRetryCommitsOneMovement() throws Exception {
        // This method's fixture is committed only in the isolated H2 database above.
        var req=request("ENTRADA",3);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> job=()->{authenticate();try{return service.registrarAjuste(req).getCantidad();}finally{SecurityContextHolder.clearContext();}};
            var first=pool.submit(job); var second=pool.submit(job);
            assertEquals(13,first.get(15,TimeUnit.SECONDS));assertEquals(13,second.get(15,TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM rya.movimientos_inventario WHERE existencia_id=?",Integer.class,stock));
            assertEquals(13,jdbc.queryForObject("SELECT cantidad FROM rya.existencias WHERE id=?",Integer.class,stock));
        } finally {
            pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);
            jdbc.update("DELETE FROM rya.movimientos_inventario WHERE existencia_id=?",stock);
            jdbc.update("DELETE FROM rya.existencias WHERE producto_id=?",product);
            jdbc.update("DELETE FROM rya.productos WHERE id=?",product);
            jdbc.update("DELETE FROM rya.almacenes WHERE id=?",warehouse);
            jdbc.update("DELETE FROM rya.categorias WHERE id=?",category);
            jdbc.update("DELETE FROM rya.tipos_producto WHERE id=?",type);
        }
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void failedHistoryInsertRollsBackStock() {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("test-only audit failure"))
            .when(movements).save(org.mockito.ArgumentMatchers.any(MovimientoInventario.class));
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.registrarAjuste(request("ENTRADA",3)));
            assertEquals(10,jdbc.queryForObject("SELECT cantidad FROM rya.existencias WHERE id=?",Integer.class,stock));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM rya.movimientos_inventario WHERE existencia_id=?",Integer.class,stock));
        } finally {
            org.mockito.Mockito.reset(movements);
            jdbc.update("DELETE FROM rya.existencias WHERE producto_id=?",product);
            jdbc.update("DELETE FROM rya.productos WHERE id=?",product);
            jdbc.update("DELETE FROM rya.almacenes WHERE id=?",warehouse);
            jdbc.update("DELETE FROM rya.categorias WHERE id=?",category);
            jdbc.update("DELETE FROM rya.tipos_producto WHERE id=?",type);
        }
    }
}
