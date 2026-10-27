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
    @Autowired ProductImportService imports;
    @Autowired FiltroService filters;
    @Autowired ProductoRepository productRepo;
    @Autowired CategoriaRepository categories;
    @Autowired TipoProductoRepository types;
    @Autowired AtributoTecnicoRepository attributes;
    @Autowired ValorAtributoProductoRepository values;
    @Autowired EspecificacionCpuRepository cpus;
    @Autowired RevisionPlacaRepository boards;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.test.context.bean.override.mockito.MockitoBean CatalogAttributeRestorer attributeRestorer;

    @Test void limpiezaParcialConservaFichaYFuenteActualesYNoExponeNotasInternas(){
        var dto=request(category("Mouse"),new FichaProductoDto("mouse","https://example.com/source",Map.of("max_dpi",8000,"silent_clicks",false,"connections","Bluetooth","variant_notes","Revisar referencia")));
        dto.setStockInicial(30);var created=products.crearProducto(dto);
        var fields=new LinkedHashMap<String,Object>();fields.put("variant_notes",null);fields.put("internal_notes","Revisar referencia; comentario solo administrativo.");
        products.actualizarCamposFichaProducto(created.getId(),created.getSku(),new FichaProductoDto("mouse",null,fields),List.of("variant_notes","internal_notes"));
        var admin=products.obtenerPorIdAdmin(created.getId());assertEquals("https://example.com/source",admin.getFichaTecnica().fuenteUrl());
        assertEquals(8000,((Number)admin.getFichaTecnica().valores().get("max_dpi")).intValue());assertEquals(false,admin.getFichaTecnica().valores().get("silent_clicks"));
        assertEquals("Bluetooth",admin.getFichaTecnica().valores().get("connections"));assertFalse(admin.getFichaTecnica().valores().containsKey("variant_notes"));
        assertTrue(admin.getFichaTecnica().valores().get("internal_notes").toString().contains("administrativo"));assertEquals(30,admin.getStockDisponible());
        assertTrue(products.obtenerPorId(created.getId()).getAtributos().stream().noneMatch(a->a.getCodigo().endsWith("_INTERNAL_NOTES")));
        assertTrue(products.obtenerPorSlug(created.getSlug()).getAtributos().stream().noneMatch(a->a.getCodigo().endsWith("_INTERNAL_NOTES")));
        assertFalse(ProductTechnicalData.isPublicFilterCode("CATALOG_TECH_MOUSE_INTERNAL_NOTES"));
        assertTrue(ProductTechnicalData.schema(category("Mouse"),categories).campos().stream().anyMatch(field->field.key().equals("internal_notes")));
    }

    @Test void limpiezaParcialRechazaSkuDistintoOCamposNoDeclarados(){
        var created=products.crearProducto(request(category("Mouse"),null));
        var patch=new FichaProductoDto("mouse",null,Map.of("internal_notes","Comentario"));
        assertThrows(BadRequestException.class,()->products.actualizarCamposFichaProducto(created.getId(),"OTRO",patch,List.of("internal_notes")));
    }

    @Test void skuYSlugEliminadosQuedanReservadosSinReactivacionPorImportacion(){
        var dto=request(category("Mouse"),null);var created=products.crearProducto(dto);products.eliminarProducto(created.getId());
        var skuError=assertThrows(com.ecommerce.backend.exception.ConflictException.class,()->products.crearProducto(dto));assertTrue(skuError.getMessage().contains("eliminado"));
        dto.setSku("NEW-"+UUID.randomUUID());dto.setSlug(created.getSlug());
        var slugError=assertThrows(com.ecommerce.backend.exception.ConflictException.class,()->products.crearProducto(dto));assertTrue(slugError.getMessage().contains("slug"));
        assertTrue(productRepo.findById(created.getId()).orElseThrow().getIsDeleted());
    }

    @Test void completarDuplicadoConservaCeroFalseStockPublicacionYPrincipal(){
        var dto=request(category("Procesadores"),new FichaProductoDto("cpu","https://example.com/old",Map.of("cores",6,"igpu",false,"tdp_w",0)));
        dto.setPrecioBase(BigDecimal.ZERO);dto.setStockInicial(30);dto.setEstado("active");dto.setRequiereSerie(true);dto.setRequiereEnvio(false);
        dto.setImagenes(List.of(ImagenProductoDto.builder().url("https://example.com/old.webp").principal(true).posicion(0).build()));
        var created=products.crearProducto(dto);var before=imports.preview(created.getId());
        var incoming=new ProductImportService.Data("Otro nombre","Descripción nueva",new BigDecimal("99"),null,
            new FichaProductoDto("cpu","https://example.com/new",Map.of("cores",8,"igpu",true,"tdp_w",65,"threads",12)),
            List.of(ImagenProductoDto.builder().url("https://example.com/old.webp").build(),ImagenProductoDto.builder().url("https://example.com/new.webp").build()));
        var after=imports.resolve(created.getId(),new ProductImportService.Resolution(created.getSku(),before.revision(),"completar",Set.of(),incoming)).actual();
        assertEquals(created.getId(),after.getId());assertEquals(created.getNombre(),after.getNombre());assertEquals(0,after.getPrecioBase().signum());
        assertEquals(30,after.getStockDisponible());assertTrue(after.getActivo());assertTrue(after.getVendible());assertTrue(after.getRequiereSerie());assertFalse(after.getRequiereEnvio());
        assertEquals("Descripción nueva",after.getDescripcion());assertEquals(false,after.getFichaTecnica().valores().get("igpu"));
        assertEquals(6,((Number)after.getFichaTecnica().valores().get("cores")).intValue());assertEquals(0,((Number)after.getFichaTecnica().valores().get("tdp_w")).intValue());
        assertEquals(12,((Number)after.getFichaTecnica().valores().get("threads")).intValue());assertEquals("https://example.com/old",after.getFichaTecnica().fuenteUrl());
        assertEquals(2,after.getImagenes().size());assertEquals(created.getImagenes().getFirst().getId(),after.getImagenes().getFirst().getId());assertTrue(after.getImagenes().getFirst().getPrincipal());
    }

    @Test void reemplazaSoloCamposElegidosYBloqueaComparacionesObsoletasYSkuDistinto(){
        var dto=request(category("Procesadores"),null);dto.setDescripcion("Conservar");dto.setStockInicial(30);dto.setRequiereEnvio(false);
        var created=products.crearProducto(dto);var before=imports.preview(created.getId());
        var incoming=new ProductImportService.Data("Nombre nuevo","No reemplazar",new BigDecimal("999"),null,null,List.of());
        var after=imports.resolve(created.getId(),new ProductImportService.Resolution(created.getSku(),before.revision(),"actualizar",Set.of("nombre"),incoming)).actual();
        assertEquals("Nombre nuevo",after.getNombre());assertEquals("Conservar",after.getDescripcion());assertEquals(0,BigDecimal.TEN.compareTo(after.getPrecioBase()));assertEquals(30,after.getStockDisponible());assertFalse(after.getRequiereEnvio());
        assertThrows(com.ecommerce.backend.exception.ConflictException.class,()->imports.resolve(created.getId(),new ProductImportService.Resolution(created.getSku(),before.revision(),"actualizar",Set.of("nombre"),incoming)));
    }

    @Test void rechazaCamposProtegidosYNoUsaNombreParaVincular(){
        var created=products.crearProducto(request(category("Mouse"),null));var before=imports.preview(created.getId());
        var incoming=new ProductImportService.Data("Nuevo",null,BigDecimal.TEN,null,null,null);
        assertThrows(BadRequestException.class,()->imports.resolve(created.getId(),new ProductImportService.Resolution(created.getSku(),before.revision(),"actualizar",Set.of("stockInicial"),incoming)));
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void falloDeGaleriaRevierteTambienLosDatosComercialesDelDuplicado(){
        var dto=request(category("Mouse"),null);dto.setDescripcion(null);
        dto.setImagenes(java.util.stream.IntStream.range(0,8).mapToObj(i->ImagenProductoDto.builder().url("https://example.com/"+i).principal(i==0).posicion(i).build()).toList());
        var created=products.crearProducto(dto);var before=imports.preview(created.getId());
        var incoming=new ProductImportService.Data(null,"No debe guardarse",null,null,null,List.of(ImagenProductoDto.builder().url("https://example.com/nueva").build()));
        assertThrows(BadRequestException.class,()->imports.resolve(created.getId(),new ProductImportService.Resolution(created.getSku(),before.revision(),"completar",Set.of(),incoming)));
        var after=products.obtenerPorIdAdmin(created.getId());assertNull(after.getDescripcion());assertEquals(8,after.getImagenes().size());
    }

    @Test void filtrosComercialesDependenDeCategoriaYValoresPublicadosSinMostrarMetadatos(){
        var mouse=category("Mouse");
        var first=products.crearProducto(request(mouse,new FichaProductoDto("mouse",null,Map.of("color","Negro","sensor","Óptico","max_dpi",8000,"variant_notes","Nota interna"))));
        var second=products.crearProducto(request(mouse,new FichaProductoDto("mouse",null,Map.of("color","Blanco","max_dpi",16000))));
        products.cambiarEstado(first.getId(),"publicar");products.cambiarEstado(second.getId(),"publicar");
        products.crearProducto(request(mouse,new FichaProductoDto("mouse",null,Map.of("color","Color de borrador"))));
        var keyboard=products.crearProducto(request(category("Teclados"),new FichaProductoDto("keyboard",null,Map.of("layout","Español"))));
        products.cambiarEstado(keyboard.getId(),"publicar");
        var facets=filters.obtenerFiltrosDisponibles(mouse.getSlug()).getAtributos();
        var colors=facets.stream().filter(a->a.getCodigo().equals("CATALOG_TECH_MOUSE_COLOR")).findFirst().orElseThrow();
        assertEquals(Set.of("Blanco","Negro"),new HashSet<>(colors.getValores()));assertEquals("Color",colors.getNombre());
        assertTrue(facets.stream().noneMatch(a->Set.of("CATALOG_TECH_TYPE","CATALOG_TECH_STATUS","CATALOG_TECH_SOURCE","CATALOG_TECH_MOUSE_VARIANT_NOTES","CATALOG_TECH_KEYBOARD_LAYOUT").contains(a.getCodigo())));
        var result=products.listarProductos(null,null,mouse.getSlug(),null,null,null,null,null,Map.of("CATALOG_TECH_MOUSE_COLOR",List.of("Negro")),0,12,"nombre","asc");
        assertEquals(1,result.getTotalElements());assertEquals(first.getId(),result.getContent().getFirst().getId());
        var both=products.listarProductos(null,null,mouse.getSlug(),null,null,null,null,null,Map.of("CATALOG_TECH_MOUSE_COLOR",List.of("Negro","Blanco")),0,12,"nombre","asc");
        assertEquals(2,both.getTotalElements());
        assertFalse(attributes.findByCodigo("CATALOG_TECH_MOUSE_COLOR").orElseThrow().getFiltrable());
        assertThrows(BadRequestException.class,()->products.listarProductos(null,null,mouse.getSlug(),null,null,null,null,null,Map.of("CATALOG_TECH_STATUS",List.of("Pendiente")),0,12,"nombre","asc"));
    }

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
