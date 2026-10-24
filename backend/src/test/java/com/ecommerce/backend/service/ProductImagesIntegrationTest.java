package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class ProductImagesIntegrationTest {
    @Autowired ProductoService products;
    @Autowired CategoriaRepository categories;
    @Autowired TipoProductoRepository types;
    @Autowired ImagenProductoRepository images;

    private AdminProductoDetalleDto product(){
        var category=categories.save(Categoria.builder().nombre("Procesadores").slug("images-"+UUID.randomUUID()).build());
        var type=types.save(TipoProducto.builder().codigo("T"+UUID.randomUUID().toString().substring(0,8)).build());
        return products.crearProducto(AdminProductoDto.builder().nombre("CPU imágenes").sku("IMG-"+UUID.randomUUID()).categoriaId(category.getId()).tipoId(type.getId()).precioBase(BigDecimal.TEN).stockInicial(0).estado("draft").descripcion("No modificar").fichaTecnica(new FichaProductoDto("cpu",null,Map.of("cores",6))).build());
    }
    private ImagenProductoDto image(String name){return ImagenProductoDto.builder().url("https://example.com/"+name+".webp").build();}

    @Test void agregaEnOrdenSinCambiarDatosComercialesFichaNiPrincipalActualYReintentarNoDuplica(){
        var p=product();var first=products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(image("one")));
        var saved=products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(image("one"),image("two"),image("three")));
        assertEquals(3,saved.getImagenes().size());assertEquals(first.getImagenes().getFirst().getId(),saved.getImagenes().getFirst().getId());
        assertTrue(saved.getImagenes().getFirst().getPrincipal());assertFalse(saved.getImagenes().get(1).getPrincipal());assertEquals(2,saved.getImagenes().get(2).getPosicion());
        assertEquals(p.getPrecioBase(),saved.getPrecioBase());assertEquals(p.getStockDisponible(),saved.getStockDisponible());assertEquals(p.getActivo(),saved.getActivo());assertEquals(p.getVendible(),saved.getVendible());
        assertEquals("No modificar",saved.getDescripcion());assertEquals(p.getFichaTecnica(),saved.getFichaTecnica());
        products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(image("two"),image("three")));assertEquals(3,images.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(p.getId()).size());
    }
    @Test void superarOchoEnTotalRechazaElProductoCompletoSinBorrarNiGuardarParcialmente(){
        var p=product();var seven=java.util.stream.IntStream.range(0,7).mapToObj(i->image("old-"+i)).toList();products.agregarImagenesProducto(p.getId(),p.getSku(),seven);
        assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(image("eight"),image("nine"))));
        assertEquals(7,images.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(p.getId()).size());
    }
    @Test void validaSkuCantidadHttpsCredencialesYRepetidosAntesDeGuardar(){
        var p=product();
        assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),"OTHER",List.of(image("one"))));
        assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),p.getSku(),List.of()));
        for(String url:List.of("http://example.com/a","https://user:secret@example.com/a","javascript:alert(1)"))assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(ImagenProductoDto.builder().url(url).build())));
        assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),p.getSku(),List.of(image("one"),image("one"))));
        assertThrows(BadRequestException.class,()->products.agregarImagenesProducto(p.getId(),p.getSku(),java.util.stream.IntStream.range(0,9).mapToObj(i->image("n-"+i)).toList()));
        assertTrue(images.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(p.getId()).isEmpty());
    }
}
