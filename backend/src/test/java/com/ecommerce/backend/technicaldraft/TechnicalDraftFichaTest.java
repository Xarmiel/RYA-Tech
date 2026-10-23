package com.ecommerce.backend.technicaldraft;

import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TechnicalDraftFichaTest {
    final ObjectMapper mapper=new ObjectMapper();
    final ProductoRepository products=mock(ProductoRepository.class);
    final EspecificacionCpuRepository cpus=mock(EspecificacionCpuRepository.class);
    final TechnicalDraftFicha service=new TechnicalDraftFicha(products,cpus,mapper,mock(org.springframework.jdbc.core.JdbcTemplate.class));
    Producto product(){return Producto.builder().id(UUID.randomUUID()).sku("TEST-CPU").nombre("CPU real")
        .categoria(Categoria.builder().slug("procesadores").build()).build();}
    @Test void comparesRealNormalizedCpuPreservingFalseAndNullWithoutWriting() throws Exception {
        var p=product();when(products.findBySkuAndIsDeletedFalse("TEST-CPU")).thenReturn(Optional.of(p));
        when(cpus.findById(p.getId())).thenReturn(Optional.of(EspecificacionCpu.builder().socket("AM5").nucleos(8).hilos(16).potenciaBaseW(new BigDecimal("65.0000")).tieneGraficosIntegrados(false).build()));
        var doc=TechnicalDraftValidatorTest.document(mapper);var preview=service.preview(doc);
        assertFalse(preview.linked());assertEquals(p.getId(),preview.productId());
        assertEquals("SAME",preview.fields().stream().filter(f->f.key().equals("igpu")).findFirst().orElseThrow().status());
        assertEquals("SAME",preview.fields().stream().filter(f->f.key().equals("base_w")).findFirst().orElseThrow().status());
        assertTrue(preview.fields().stream().filter(f->f.key().equals("max_w")).findFirst().orElseThrow().current().isNull());
        verify(products,never()).save(any());verify(cpus,never()).save(any());
    }
    @Test void stableLinkCannotBeReusedForAnotherSkuOrType() throws Exception {
        var p=product();when(products.findByIdAndIsDeletedFalse(p.getId())).thenReturn(Optional.of(p));
        var doc=TechnicalDraftValidatorTest.document(mapper);doc.put("catalogProductId",p.getId().toString());
        ((com.fasterxml.jackson.databind.node.ObjectNode)doc.get("product")).put("sku","OTHER");
        assertEquals(409,assertThrows(DraftFault.class,()->service.resolve(doc)).status);
        ((com.fasterxml.jackson.databind.node.ObjectNode)doc.get("product")).put("sku","TEST-CPU").put("type","board");
        assertEquals(409,assertThrows(DraftFault.class,()->service.resolve(doc)).status);
    }
    @Test void absentProductIsNotInventedAndUnlinkedDraftsRemainAllowed() throws Exception {
        var doc=TechnicalDraftValidatorTest.document(mapper);service.validateLink(doc);verifyNoInteractions(products,cpus);
        assertEquals(404,assertThrows(DraftFault.class,()->service.preview(doc)).status);
    }
    @Test void invalidLinkIsRejectedBeforeResolving() throws Exception {
        var doc=TechnicalDraftValidatorTest.document(mapper);doc.put("catalogProductId","not-a-uuid");
        assertFalse(new TechnicalDraftValidator(mapper).validate(doc,false).valid());
    }
}
