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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductTechnicalDataIntegrationTest {
    @Autowired ProductoService products;
    @Autowired ProductoRepository productRepo;
    @Autowired CategoriaRepository categories;
    @Autowired TipoProductoRepository types;
    @Autowired AtributoTecnicoRepository attributes;
    @Autowired ValorAtributoProductoRepository values;
    @Autowired EspecificacionCpuRepository cpus;
    @Autowired RevisionPlacaRepository boards;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.test.context.bean.override.mockito.MockitoBean CatalogAttributeRestorer attributeRestorer;

    private void legacy(UUID id,String code,Object value,String unit){
        var attribute=attributes.findByCodigo(code).orElseGet(()->attributes.save(AtributoTecnico.builder().codigo(code).nombre(code).tipoDato(value instanceof Number?"NUMERO":value instanceof Boolean?"BOOLEANO":"TEXTO").unidad(unit).isDeleted(false).build()));
        var row=ValorAtributoProducto.builder().producto(productRepo.findById(id).orElseThrow()).atributo(attribute).isDeleted(false).build();
        if(value instanceof Number n)row.setValorNumero(new BigDecimal(n.toString()));else if(value instanceof Boolean b)row.setValorBooleano(b);else row.setValorTexto((String)value);values.save(row);
    }
    @Test void recuperaFichaAnteriorCpuSinEscriturasNiPerderFalseYConvierteKBAMb(){
        var request=request(category("Procesadores"),null);var created=products.crearProducto(request);
        legacy(created.getId(),"socket_cpu","AM4",null);legacy(created.getId(),"nucleos_cpu",6,null);
        legacy(created.getId(),"cache_l3_kb",32768,"KB");legacy(created.getId(),"graficos_integrados_cpu",false,null);legacy(created.getId(),"potencia_base_w",65,"W");
        var before=values.count();var ficha=products.obtenerPorIdAdmin(created.getId()).getFichaTecnica();
        assertEquals("ANTERIOR",ficha.origen());assertEquals("AM4",ficha.valores().get("socket"));assertEquals(false,ficha.valores().get("igpu"));
        assertEquals(0,new BigDecimal("32").compareTo((BigDecimal)ficha.valores().get("l3_mb")));assertFalse(ficha.valores().containsKey("max_w"));
        assertFalse(ficha.valores().containsKey("tdp_w"));assertTrue(ficha.avisos().stream().anyMatch(n->n.contains("no se reclasifica")));assertEquals(before,values.count());
    }
    @Test void recuperaFuenteYDatosNormalizadosYAdvierteConflictosSinSobrescribirlos(){
        var request=request(category("Procesadores"),null);var id=products.crearProducto(request).getId();var source=UUID.randomUUID();
        jdbc.update("insert into rya.fuentes_tecnicas(id,titulo,url) values (?,?,?)",source,"Fabricante","https://example.com/cpu");
        cpus.save(EspecificacionCpu.builder().productoId(id).modelo("CPU prueba").familia("Familia").generacion("Generación").socket("AM4").nucleos(6).hilos(12).potenciaBaseW(new BigDecimal("65")).potenciaMaximaW(new BigDecimal("88")).tieneGraficosIntegrados(false).fuenteId(source).verificadaAt(java.time.OffsetDateTime.now()).build());
        legacy(id,"nucleos_cpu",8,null);
        var ficha=products.obtenerPorIdAdmin(id).getFichaTecnica();assertEquals(6,ficha.valores().get("cores"));assertEquals("https://example.com/cpu",ficha.fuenteUrl());assertTrue(ficha.avisos().stream().anyMatch(n->n.contains("Diferencia en cores")));
        request.setFichaTecnica(ficha);products.actualizarProducto(id,request);
        assertEquals("COMERCIAL",products.obtenerPorIdAdmin(id).getFichaTecnica().origen());assertEquals(6,cpus.findById(id).orElseThrow().getNucleos());
        assertEquals(8,values.findConAtributoPorProductoId(id).stream().filter(v->v.getAtributo().getCodigo().equals("nucleos_cpu")).findFirst().orElseThrow().getValorNumero().intValue());
    }
    @Test void fichaComercialVaciaNoResucitaValoresHeredadosBorradosDelFormulario(){
        var request=request(category("Procesadores"),null);var id=products.crearProducto(request).getId();legacy(id,"socket_cpu","AM4",null);
        request.setFichaTecnica(new FichaProductoDto("cpu",null,Map.of()));products.actualizarProducto(id,request);
        assertEquals("COMERCIAL",products.obtenerPorIdAdmin(id).getFichaTecnica().origen());assertTrue(products.obtenerPorIdAdmin(id).getFichaTecnica().valores().isEmpty());
    }
    @Test void placaRecuperaSusCamposAdicionalesYNoEligeRevisionSiHayVarias(){
        var request=request(category("Placas Madre"),null);var id=products.crearProducto(request).getId();
        legacy(id,"wifi_integrado_placa",true,null);legacy(id,"capacidad_ram_max_placa_gb",128,"GB");legacy(id,"ranuras_m2_totales",2,null);
        for(var revision:List.of("1.0","2.0")) boards.save(RevisionPlaca.builder().productoId(id).modelo("Placa").revision(revision).socket("AM4").chipset("B550").fuenteId(UUID.randomUUID()).verificadaAt(java.time.OffsetDateTime.now()).build());
        var ficha=products.obtenerPorIdAdmin(id).getFichaTecnica();assertEquals(true,ficha.valores().get("wifi"));assertEquals(2,((BigDecimal)ficha.valores().get("m2_slots")).intValue());assertFalse(ficha.valores().containsKey("revision"));assertTrue(ficha.avisos().stream().anyMatch(n->n.contains("varias revisiones")));
        request.setFichaTecnica(ficha);assertDoesNotThrow(()->products.actualizarProducto(id,request));
    }
    @Test void perifericosTienenFichaSinRelacionCpuPlaca(){
        var keyboard=request(category("Teclados"),new FichaProductoDto("keyboard",null,Map.of("wireless",true,"devices",3)));
        assertEquals("keyboard",products.crearProducto(keyboard).getFichaTecnica().tipo());
        var mouse=request(category("Mouse"),new FichaProductoDto("mouse",null,Map.of("max_dpi",8000,"silent_clicks",true)));
        assertEquals("mouse",products.crearProducto(mouse).getFichaTecnica().tipo());
    }
    @Test void actualizacionSoloFichaExigeSkuExactoYConservaElProductoComercial(){
        var request=request(category("Procesadores"),null);request.setDescripcion("Descripción conservada");
        var created=products.crearProducto(request);var id=created.getId();
        var ficha=new FichaProductoDto("cpu","https://example.com/cpu",Map.of("cores",6,"igpu",false));
        assertThrows(BadRequestException.class,()->products.actualizarFichaProducto(id,"OTRO-SKU",ficha));
        assertThrows(BadRequestException.class,()->products.actualizarFichaProducto(id,created.getSku(),null));
        assertTrue(values.findConAtributoPorProductoId(id).isEmpty());
        var saved=products.actualizarFichaProducto(id,created.getSku(),ficha);
        assertEquals(created.getPrecioBase(),saved.getPrecioBase());assertEquals(created.getStockDisponible(),saved.getStockDisponible());
        assertEquals(created.getActivo(),saved.getActivo());assertEquals(created.getVendible(),saved.getVendible());
        assertEquals("Descripción conservada",saved.getDescripcion());assertEquals(created.getImagenes(),saved.getImagenes());
        assertEquals(false,saved.getFichaTecnica().valores().get("igpu"));
    }
    @Test void ejemplosDeLosDosJsonUsanCamposYUnidadesAceptadosPorElBackend() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        try(var stream=getClass().getResourceAsStream("/technical-import-examples.json")) {
            var document=mapper.readTree(stream);assertEquals(23,document.path("productos").size());
            for(var item:document.path("productos")) {
                var dto=mapper.treeToValue(item.path("fichaTecnica"),FichaProductoDto.class);
                var request=request(category(item.path("categoria").asText()),null);var product=products.crearProducto(request);
                var saved=products.actualizarFichaProducto(product.getId(),product.getSku(),dto);
                assertEquals(dto.tipo(),saved.getFichaTecnica().tipo());assertEquals(dto.valores().keySet(),saved.getFichaTecnica().valores().keySet());
            }
        }
    }

    private Categoria category(String name){return categories.save(Categoria.builder().nombre(name).slug("tech-"+UUID.randomUUID()).build());}
    private AdminProductoDto request(Categoria category,FichaProductoDto technical){
        var type=types.save(TipoProducto.builder().codigo("T"+UUID.randomUUID().toString().substring(0,8)).descripcion("Prueba").build());
        return AdminProductoDto.builder().nombre("Producto técnico de prueba").sku("TECH-"+UUID.randomUUID()).categoriaId(category.getId()).tipoId(type.getId()).precioBase(BigDecimal.TEN).estado("draft").stockInicial(0).fichaTecnica(technical).build();
    }
    @Test void seisFamiliasTienenEsquemasDiferentes(){
        var names=List.of("Procesadores","Placas Madre","Memorias RAM","Almacenamiento","Tarjetas Gráficas","Fuentes de Poder");var expected=List.of("cpu","board","ram","storage","gpu","psu");
        for(int i=0;i<names.size();i++){var schema=ProductTechnicalData.schema(category(names.get(i)),categories);assertNotNull(schema);assertEquals(expected.get(i),schema.tipo());assertFalse(schema.campos().isEmpty());}
    }
    @Test void subcategoriaHeredaEsquemaSinInventarTipoParaPerifericos(){
        var parent=category("Procesadores");var child=category("AMD AM4");child.setParentId(parent.getId());categories.save(child);assertEquals("cpu",ProductTechnicalData.schema(child,categories).tipo());assertNull(ProductTechnicalData.schema(category("Audífonos"),categories));
    }
    @Test void crearLeerYEditarPersisteValoresYReutilizaFilasEliminadas(){
        var request=request(category("Procesadores"),new FichaProductoDto("cpu","https://example.com/manufacturer",Map.of("socket","AM4","cores",6,"tdp_w",65,"igpu",false)));
        var created=products.crearProducto(request);assertNotNull(created.getFichaTecnica());assertEquals(false,created.getFichaTecnica().valores().get("igpu"));assertEquals(0,((BigDecimal)created.getFichaTecnica().valores().get("tdp_w")).compareTo(new BigDecimal("65")));
        assertEquals("https://example.com/manufacturer",products.obtenerPorIdAdmin(created.getId()).getFichaTecnica().fuenteUrl());
        request.setFichaTecnica(new FichaProductoDto("cpu",null,Map.of("cores",8,"tdp_w",40)));products.actualizarProducto(created.getId(),request);assertFalse(products.obtenerPorIdAdmin(created.getId()).getFichaTecnica().valores().containsKey("socket"));
        request.setFichaTecnica(new FichaProductoDto("cpu",null,Map.of("socket","AM4","cores",8)));products.actualizarProducto(created.getId(),request);assertEquals("AM4",products.obtenerPorIdAdmin(created.getId()).getFichaTecnica().valores().get("socket"));
        assertEquals(1,values.findAllByProducto_Id(created.getId()).stream().filter(row->row.getAtributo().getCodigo().equals("CATALOG_TECH_CPU_SOCKET")).count());
        org.mockito.Mockito.verify(attributeRestorer).restore(org.mockito.ArgumentMatchers.argThat(row->row.getAtributo().getCodigo().equals("CATALOG_TECH_CPU_SOCKET")));
    }
    @Test void detallePublicoMuestraUnidadYEstadoPendienteSinActivarFiltrosCertificados(){
        var request=request(category("Memorias RAM"),new FichaProductoDto("ram",null,Map.of("memory_type","DDR4","modules",2,"speed_mts",3200)));var created=products.crearProducto(request);products.cambiarEstado(created.getId(),"publicar");var detail=products.obtenerPorId(created.getId());
        assertTrue(detail.getAtributos().stream().anyMatch(a->a.getCodigo().equals("CATALOG_TECH_RAM_SPEED_MTS")&&"3200".equals(a.getValor())&&"MT/s".equals(a.getUnidad())));assertTrue(detail.getAtributos().stream().anyMatch(a->a.getCodigo().equals("CATALOG_TECH_STATUS")&&a.getValor().contains("no certifica")));assertFalse(attributes.findByCodigo("CATALOG_TECH_RAM_SPEED_MTS").orElseThrow().getFiltrable());
    }
    @Test void noModificaAtributosPreviosNiInterpretaAusenciaComoCero(){
        var request=request(category("Procesadores"),null);var created=products.crearProducto(request);var product=productRepo.findById(created.getId()).orElseThrow();var legacy=attributes.save(AtributoTecnico.builder().codigo("LEGACY_"+UUID.randomUUID()).nombre("Dato anterior").tipoDato("NUMERO").build());values.save(ValorAtributoProducto.builder().producto(product).atributo(legacy).valorNumero(new BigDecimal("99")).build());
        request.setFichaTecnica(new FichaProductoDto("cpu",null,Map.of()));products.actualizarProducto(created.getId(),request);assertTrue(values.findConAtributoPorProductoId(created.getId()).stream().anyMatch(row->row.getAtributo().getId().equals(legacy.getId())&&row.getValorNumero().intValue()==99));assertTrue(products.obtenerPorIdAdmin(created.getId()).getFichaTecnica().valores().isEmpty());
    }
    @Test void omitirFichaEnEdicionComercialLaConserva(){
        var request=request(category("Procesadores"),new FichaProductoDto("cpu",null,Map.of("cores",6)));var created=products.crearProducto(request);request.setFichaTecnica(null);request.setNombre("Nombre comercial nuevo");products.actualizarProducto(created.getId(),request);assertTrue(products.obtenerPorIdAdmin(created.getId()).getFichaTecnica().valores().containsKey("cores"));
    }
    @Test void cambiarFamiliaLimpiaSoloLaFichaComercialAnterior(){
        var request=request(category("Procesadores"),new FichaProductoDto("cpu",null,Map.of("cores",6)));var created=products.crearProducto(request);request.setCategoriaId(category("Fuentes de Poder").getId());request.setFichaTecnica(null);products.actualizarProducto(created.getId(),request);assertNull(products.obtenerPorIdAdmin(created.getId()).getFichaTecnica());
    }
    @Test void validaTipoCamposValoresYFuentesAntesDeGuardarAtributos(){
        var request=request(category("Procesadores"),null);var created=products.crearProducto(request);var product=productRepo.findById(created.getId()).orElseThrow();var service=new ProductTechnicalData(attributes,values,categories);
        for(var bad:List.of(new FichaProductoDto("psu",null,Map.of()),new FichaProductoDto("cpu",null,Map.of("verified",true)),new FichaProductoDto("cpu",null,Map.of("cores",1.5)),new FichaProductoDto("cpu",null,Map.of("tdp_w",-1)),new FichaProductoDto("cpu",null,Map.of("tdp_w","40")),new FichaProductoDto("cpu","http://example.com",Map.of()),new FichaProductoDto("cpu","https://user:secret@example.com",Map.of()),new FichaProductoDto("cpu",null,Map.of("igpu","false"))))assertThrows(BadRequestException.class,()->service.save(product,bad));assertTrue(values.findConAtributoPorProductoId(product.getId()).isEmpty());
    }
}
