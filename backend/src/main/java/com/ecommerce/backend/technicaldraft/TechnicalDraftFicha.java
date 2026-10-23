package com.ecommerce.backend.technicaldraft;

import com.ecommerce.backend.entity.Producto;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.EspecificacionCpuRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/** Read-only view of normalized records. No inference of compatibility or automatic writes. */
@Service
public class TechnicalDraftFicha {
    public record Field(String key,String unit,JsonNode current,JsonNode proposed,String status) {}
    public record View(UUID productId,String sku,String name,boolean linked,String sourceId,String sourceUrl,String verifiedAt,
                       List<Field> fields,List<String> warnings) {}
    private final ProductoRepository products;
    private final EspecificacionCpuRepository cpus;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    public TechnicalDraftFicha(ProductoRepository products,EspecificacionCpuRepository cpus,ObjectMapper mapper,JdbcTemplate jdbc){
        this.products=products;this.cpus=cpus;this.mapper=mapper;this.jdbc=jdbc;
    }
    @Transactional(readOnly=true)
    public Producto resolve(JsonNode doc){
        String id=doc.path("catalogProductId").asText("");
        var product=id.isBlank()?products.findBySkuAndIsDeletedFalse(doc.path("product").path("sku").asText().trim())
            :products.findByIdAndIsDeletedFalse(UUID.fromString(id));
        Producto p=product.orElseThrow(()->new DraftFault(404,"No existe un producto vigente con ese SKU o vínculo. Crea primero el producto comercial."));
        if(!p.getSku().equals(doc.path("product").path("sku").asText().trim()))throw new DraftFault(409,"El SKU no coincide con el producto vinculado. Desvincula antes de cambiarlo.");
        String expected=switch(p.getCategoria().getSlug()){case "procesadores"->"cpu";case "placas-madre"->"board";default->null;};
        if(expected!=null&&!expected.equals(doc.path("product").path("type").asText()))throw new DraftFault(409,"El tipo del borrador no coincide con la categoría del producto real.");
        return p;
    }
    public void validateLink(JsonNode doc){if(!doc.path("catalogProductId").asText("").isBlank())resolve(doc);}
    @Transactional(readOnly=true)
    public View preview(JsonNode doc){
        Producto p=resolve(doc);Map<String,Object> values=new LinkedHashMap<>();Map<String,String> units=new HashMap<>();
        // Explicit, dimensionally equivalent EAV mappings only. No MHz <-> MT/s conversion.
        Map<String,String> mapping=doc.path("product").path("type").asText().equals("cpu")
            ?Map.of("socket_cpu","socket","nucleos_cpu","cores","hilos_cpu","threads","frecuencia_base_mhz","base_mhz","frecuencia_maxima_mhz","boost_mhz","cache_l3_kb","l3_kb","potencia_base_w","base_w","graficos_integrados_cpu","igpu")
            :doc.path("product").path("type").asText().equals("board")?Map.of("socket_placa","socket","chipset_placa","chipset","formato_placa","form_factor","tipo_ram_placa","memory_type","ranuras_ram_placa","memory_slots","capacidad_ram_max_placa_gb","memory_max_gb"):Map.of();
        List<String> warnings=new ArrayList<>();
        for(var value:p.getValoresAtributos()){
            if(Boolean.TRUE.equals(value.getIsDeleted())||Boolean.TRUE.equals(value.getAtributo().getIsDeleted()))continue;
            String code=value.getAtributo().getCodigo(),key=mapping.getOrDefault(code,"attribute:"+code);
            Object stored=value.getValorNumero()!=null?value.getValorNumero():value.getValorBooleano()!=null?value.getValorBooleano():value.getValorTexto();
            if(values.containsKey(key))warnings.add("Atributo duplicado: "+code+". Resolver antes de aplicar cambios.");
            values.put(key,stored);units.put(key,value.getAtributo().getUnidad());
        }
        String source=null,verified=null;
        if(doc.path("product").path("type").asText().equals("cpu")){
            var cpu=cpus.findById(p.getId());
            if(cpu.isPresent()){
                var c=cpu.get();values.put("socket",c.getSocket());values.put("family",c.getFamilia());values.put("cores",c.getNucleos());values.put("threads",c.getHilos());
                values.put("base_mhz",c.getFrecuenciaBaseMhz());values.put("boost_mhz",c.getFrecuenciaMaximaMhz());values.put("l3_kb",c.getCacheL3Kb());
                values.put("base_w",c.getPotenciaBaseW());values.put("max_w",c.getPotenciaMaximaW());values.put("igpu",c.getTieneGraficosIntegrados());
                source=c.getFuenteId()==null?null:c.getFuenteId().toString();verified=c.getVerificadaAt()==null?null:c.getVerificadaAt().toString();
            }else warnings.add("Sin especificación CPU normalizada; no se completarán datos por inferencia.");
        }
        List<Field> fields=new ArrayList<>();Set<String> keys=new LinkedHashSet<>(values.keySet());doc.path("technical").fieldNames().forEachRemaining(keys::add);
        for(String key:keys){
            JsonNode current=mapper.valueToTree(values.get(key)),proposed=doc.path("technical").path(key);if(proposed.isMissingNode())proposed=mapper.nullNode();
            String status=proposed.isNull()?"NOT_PROPOSED":current.isNull()?"NEW":equal(current,proposed)?"SAME":"DIFFERENT";
            fields.add(new Field(key,units.get(key),current,proposed,status));
        }
        String sourceUrl=null;
        if(source!=null){var urls=jdbc.query("select url from rya.fuentes_tecnicas where id = ?",(rs,row)->rs.getString(1),UUID.fromString(source));
            if(!urls.isEmpty())sourceUrl=urls.getFirst();}
        if(mapping.isEmpty())warnings.add("Este tipo muestra atributos reales sin mapeo automático de equivalencias; no implica ficha completa.");
        warnings.add("La ficha real no se modifica. Aprobar contenido no publica productos ni certifica compatibilidad.");
        return new View(p.getId(),p.getSku(),p.getNombre(),!doc.path("catalogProductId").asText("").isBlank(),source,sourceUrl,verified,fields,warnings);
    }
    private boolean equal(JsonNode a,JsonNode b){return a.isNumber()&&b.isNumber()?a.decimalValue().compareTo(b.decimalValue())==0:a.equals(b);}
}
