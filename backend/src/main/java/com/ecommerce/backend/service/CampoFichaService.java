package com.ecommerce.backend.service;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.exception.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.OffsetDateTime;
import java.text.Normalizer;
import java.math.BigDecimal;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CampoFichaService {
 private final CategoriaRepository categories;private final CampoFichaCategoriaRepository fields;private final AtributoTecnicoRepository attributes;
 public record Request(@NotBlank @Size(max=100) String nombre,@NotBlank @Pattern(regexp="text|number|boolean") String tipoDato,@Size(max=20) String unidad,@Size(max=120) String ejemplo,boolean soloEnteros,Long version){}
 public record View(UUID id,UUID categoriaId,String categoria,String clave,String nombre,String tipoDato,String unidad,String ejemplo,boolean soloEnteros,Long version,boolean heredado){}
 public record Result(List<View> campos,ProductTechnicalData.Schema esquema){}
 private Categoria category(UUID id){return categories.findById(id).filter(c->!Boolean.TRUE.equals(c.getIsDeleted())).orElseThrow(()->new ResourceNotFoundException("Categoría no disponible"));}
 private static String normalized(String value){return Normalizer.normalize(value.trim(),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
 private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
 public Result list(UUID id){var original=category(id);var current=original;var visited=new HashSet<UUID>();List<View> result=new ArrayList<>();for(int depth=0;current!=null&&depth<20&&visited.add(current.getId());depth++){
  for(var field:fields.findByCategoriaIdOrderByCreatedAtAsc(current.getId()))result.add(new View(field.getId(),field.getCategoriaId(),current.getNombre(),field.getClave(),field.getNombre(),field.getTipoDato(),field.getUnidad(),field.getEjemplo(),field.isSoloEnteros(),field.getVersion(),!id.equals(field.getCategoriaId())));
  current=current.getParentId()==null?null:categories.findById(current.getParentId()).filter(c->!Boolean.TRUE.equals(c.getIsDeleted())).orElse(null);
 }return new Result(result,ProductTechnicalData.schema(original,categories));}
 @Transactional public Result save(UUID categoryId,UUID id,Request data,UUID actor){
  var category=categories.findForTechnicalFieldUpdate(categoryId).orElseThrow(()->new ResourceNotFoundException("Categoría no disponible"));
  String unit=clean(data.unidad()),example=clean(data.ejemplo());boolean integer=data.soloEnteros();
  if(!"number".equals(data.tipoDato())&&(integer||unit!=null))throw new BadRequestException("Solo los campos numéricos pueden tener unidad y opción de enteros.");
  if(example!=null){
   if("boolean".equals(data.tipoDato())&&!Set.of("true","false").contains(example))throw new BadRequestException("El ejemplo Sí/No debe ser true o false.");
   if("number".equals(data.tipoDato()))try{var n=new BigDecimal(example).stripTrailingZeros();if(n.scale()>4||(long)n.precision()-n.scale()>12||integer&&n.scale()>0)throw new NumberFormatException();}catch(NumberFormatException e){throw new BadRequestException("El ejemplo debe ser un número con hasta cuatro decimales; si corresponde, entero.");}
  }
  CampoFichaCategoria field;
  if(id==null){if(fields.findByCategoriaIdOrderByCreatedAtAsc(categoryId).size()>=30)throw new BadRequestException("Se permiten hasta 30 campos adicionales por categoría.");field=new CampoFichaCategoria();field.setId(UUID.randomUUID());field.setCategoriaId(categoryId);field.setClave("extra_"+field.getId().toString().replace("-",""));field.setCreadoPor(actor);field.setCreatedAt(OffsetDateTime.now());}
  else{field=fields.findById(id).filter(f->categoryId.equals(f.getCategoriaId())).orElseThrow(()->new ResourceNotFoundException("Campo no encontrado en esta categoría"));if(!Objects.equals(field.getVersion(),data.version()))throw new ConflictException("El campo cambió. Actualiza antes de editarlo.");if(!field.getTipoDato().equals(data.tipoDato())||!Objects.equals(field.getUnidad(),unit)||field.isSoloEnteros()!=integer)throw new BadRequestException("El tipo y la unidad no se cambian: crea otro campo para conservar los valores existentes.");}
  var schema=ProductTechnicalData.schema(category,categories);
  if(schema!=null&&schema.campos().stream().anyMatch(f->!f.key().equals(field.getClave())&&normalized(f.label()).equals(normalized(data.nombre()))))throw new BadRequestException("Ya existe un campo con ese nombre en la categoría o una categoría superior.");
  field.setNombre(data.nombre().trim());field.setTipoDato(data.tipoDato());field.setUnidad(unit);field.setEjemplo(example);field.setSoloEnteros(integer);field.setActualizadoPor(actor);field.setUpdatedAt(OffsetDateTime.now());fields.saveAndFlush(field);
  for(var attribute:attributes.findByCodigoEndingWithAndIsDeletedFalse("_"+field.getClave().toUpperCase(Locale.ROOT))){if(attribute.getCodigo().startsWith("CATALOG_TECH_")){attribute.setNombre("Ficha · "+field.getNombre());attributes.save(attribute);}}
  return list(categoryId);
 }
}
