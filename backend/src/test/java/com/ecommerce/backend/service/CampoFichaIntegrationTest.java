package com.ecommerce.backend.service;
import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @ActiveProfiles("test") @Transactional @AutoConfigureMockMvc
class CampoFichaIntegrationTest {
 @Autowired AdminReferenceService references;@Autowired ProductImportService imports;@Autowired CampoFichaService fields;@Autowired ProductoService products;@Autowired CategoriaRepository categories;@Autowired TipoProductoRepository types;@Autowired AtributoTecnicoRepository attributes;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;@Autowired MockMvc mvc;
 UUID actor=UUID.randomUUID();
 Categoria category(String name){return categories.save(Categoria.builder().nombre(name).slug("custom-"+UUID.randomUUID()).build());}
 AdminProductoDto request(Categoria cat,FichaProductoDto ficha){var type=types.save(TipoProducto.builder().codigo("F"+UUID.randomUUID().toString().substring(0,8)).build());return AdminProductoDto.builder().nombre("Producto con campos propios").sku("FIELD-"+UUID.randomUUID()).categoriaId(cat.getId()).tipoId(type.getId()).precioBase(BigDecimal.TEN).stockInicial(0).estado("draft").fichaTecnica(ficha).build();}
 CampoFichaService.Request field(String name,String type,String unit,String example,boolean integer,Long version){return new CampoFichaService.Request(name,type,unit,example,integer,version);}
 @Test void categoriaNuevaCreaFormularioSinCambiarElCodigoYValoresPorProducto(){
  var cat=category("Instrumentos nuevos");assertNull(ProductTechnicalData.schema(cat,categories));
  var created=fields.save(cat.getId(),null,field("Temperatura mínima","number","°C","-10.5",false,null),actor);
  var def=created.campos().getFirst();
  var metadata=references.listar(false).categorias().stream().filter(c->c.id().equals(cat.getId())).findFirst().orElseThrow();
  assertEquals("custom",metadata.fichaEsquema().tipo());assertTrue(metadata.fichaEsquema().campos().stream().anyMatch(f->f.key().equals(def.clave())));
  assertTrue(def.clave().matches("extra_[0-9a-f]{32}"));assertEquals("custom",created.esquema().tipo());assertTrue(created.esquema().campos().stream().anyMatch(f->f.custom()&&f.key().equals(def.clave())&&"-10.5".equals(f.example())));
  var empty=products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of())));assertTrue(empty.getFichaTecnica().valores().isEmpty());
  var first=products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of(def.clave(),-12.5))));
  var second=products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of(def.clave(),2.5))));
  assertEquals(-12.5,((Number)products.obtenerPorIdAdmin(first.getId()).getFichaTecnica().valores().get(def.clave())).doubleValue());assertEquals(2.5,((Number)products.obtenerPorIdAdmin(second.getId()).getFichaTecnica().valores().get(def.clave())).doubleValue());
  var snapshot=imports.preview(second.getId());
  var proposed=new ProductImportService.Data(null,null,null,null,new FichaProductoDto("custom",null,Map.of(def.clave(),5)),null);
  var imported=imports.resolve(second.getId(),new ProductImportService.Resolution(second.getSku(),snapshot.revision(),"actualizar",Set.of("fichaTecnica"),proposed)).actual();
  assertEquals(5,((Number)imported.getFichaTecnica().valores().get(def.clave())).intValue());assertEquals(second.getPrecioBase(),imported.getPrecioBase());
  products.cambiarEstado(first.getId(),"publicar");assertTrue(products.obtenerPorId(first.getId()).getAtributos().stream().anyMatch(a->a.getNombre().equals("Ficha · Temperatura mínima")&&"°C".equals(a.getUnidad())));
  assertFalse(ProductTechnicalData.isPublicFilterCode("CATALOG_TECH_CUSTOM_"+def.clave().toUpperCase(Locale.ROOT)));
 }
 @Test void heredaCamposYRenombrarConservaValoresEnFamiliasDiferentes(){
  var parent=category("Periféricos");var def=fields.save(parent.getId(),null,field("Certificación comercial","text",null,"Formato de ejemplo",false,null),actor).campos().getFirst();
  var child=category("Audífonos");child.setParentId(parent.getId());categories.saveAndFlush(child);
  var other=category("Teclados");other.setParentId(parent.getId());categories.saveAndFlush(other);
  assertTrue(fields.list(child.getId()).campos().getFirst().heredado());assertEquals("headset",fields.list(child.getId()).esquema().tipo());
  var first=products.crearProducto(request(child,new FichaProductoDto("headset",null,Map.of(def.clave(),"Dato A","microphone",false))));
  var second=products.crearProducto(request(other,new FichaProductoDto("keyboard",null,Map.of(def.clave(),"Dato B"))));
  fields.save(parent.getId(),def.id(),field("Certificación del modelo","text",null,"Otro ejemplo",false,def.version()),actor);
  assertEquals("Dato A",products.obtenerPorIdAdmin(first.getId()).getFichaTecnica().valores().get(def.clave()));assertEquals("Dato B",products.obtenerPorIdAdmin(second.getId()).getFichaTecnica().valores().get(def.clave()));
  assertTrue(attributes.findByCodigoEndingWithAndIsDeletedFalse("_"+def.clave().toUpperCase(Locale.ROOT)).stream().allMatch(a->a.getNombre().equals("Ficha · Certificación del modelo")));
  assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM rya.especificaciones_cpu WHERE producto_id=?",Long.class,first.getId()));
 }
 @Test void rechazaTipoUnidadVersionDuplicadosYClavesDeOtraCategoria(){
  var cat=category("Mouse");var def=fields.save(cat.getId(),null,field("Radio de medición","number","mm","1.5",false,null),actor).campos().getFirst();
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),null,field("Rádio de medición","text",null,null,false,null),actor));
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),null,field("Resolución máxima","text",null,null,false,null),actor));
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),def.id(),field("Radio de medición","text",null,null,false,def.version()),actor));
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),def.id(),field("Radio de medición","number","cm",null,false,def.version()),actor));
  fields.save(cat.getId(),def.id(),field("Radio declarado","number","mm","2",false,def.version()),actor);
  assertThrows(ConflictException.class,()->fields.save(cat.getId(),def.id(),field("Edición antigua","number","mm","3",false,def.version()),actor));
  assertThrows(BadRequestException.class,()->products.crearProducto(request(category("Audífonos"),new FichaProductoDto("headset",null,Map.of(def.clave(),10)))));
 }
 @Test void validaEjemplosYDatosSinUsarElEjemploComoValor(){
  var cat=category("Accesorio nuevo");
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),null,field("Conteo","number",null,"1.5",true,null),actor));
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),null,field("Booleano","boolean","W","true",false,null),actor));
  assertThrows(BadRequestException.class,()->fields.save(cat.getId(),null,field("Booleano","boolean",null,"quizás",false,null),actor));
  var def=fields.save(cat.getId(),null,field("Opción adicional","boolean",null,"true",false,null),actor).campos().getFirst();
  var unknown=products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of())));assertFalse(unknown.getFichaTecnica().valores().containsKey(def.clave()));
  var no=products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of(def.clave(),false))));assertEquals(false,no.getFichaTecnica().valores().get(def.clave()));
  assertThrows(BadRequestException.class,()->products.crearProducto(request(cat,new FichaProductoDto("custom",null,Map.of(def.clave(),"false")))));
 }
 @Test void cambiarCategoriaConservaCamposCompartidosSinPasarClavesAjenas(){
  var firstCat=category("Mouse");var secondCat=category("Ratones");
  var def=fields.save(firstCat.getId(),null,field("Prueba local","text",null,null,false,null),actor).campos().getFirst();
  var input=request(firstCat,new FichaProductoDto("mouse",null,Map.of("max_dpi",8000,def.clave(),"Solo primera categoría")));var created=products.crearProducto(input);
  input.setCategoriaId(secondCat.getId());input.setFichaTecnica(null);var moved=products.actualizarProducto(created.getId(),input);
  assertEquals(8000,((Number)moved.getFichaTecnica().valores().get("max_dpi")).intValue());assertFalse(moved.getFichaTecnica().valores().containsKey(def.clave()));
 }
 @Test void apiExigeAdministradorYNoAceptaDefinicionesInvalidas()throws Exception{
  var cat=category("Nuevos productos");String url="/api/v1/admin/categorias/"+cat.getId()+"/campos-ficha";
  mvc.perform(get(url)).andExpect(status().isUnauthorized());mvc.perform(post(url).with(jwt())).andExpect(status().isForbidden());
  mvc.perform(post(url).with(jwt().jwt(t->t.subject(actor.toString()).claim("app_metadata",Map.of("role","admin")))).contentType("application/json").content("{\"nombre\":\"Prueba\",\"tipoDato\":\"script\"}")).andExpect(status().isBadRequest());
  mvc.perform(post(url).with(jwt().jwt(t->t.subject(actor.toString()).claim("app_metadata",Map.of("role","admin")))).contentType("application/json").content("{\"nombre\":\"Texto del campo\",\"tipoDato\":\"text\",\"ejemplo\":\"Ejemplo\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.esquema.tipo").value("custom")).andExpect(jsonPath("$.esquema.campos[0].custom").value(true));
 }
}
