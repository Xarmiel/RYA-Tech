package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class TechnicalDraftValidator {
    private enum Kind { TEXT, NUMBER, INTEGER, BOOLEAN, DATE, URL }
    private record Type(Map<String,Kind> fields, Set<String> required, Set<String> groups, boolean catalogOnly) {}
    private static final Map<String,Type> TYPES = new LinkedHashMap<>();
    private static final Map<String,Map<String,Kind>> GROUPS = new LinkedHashMap<>();
    private static final Map<String,Set<String>> REQUIRED_GROUPS = new LinkedHashMap<>();
    static {
        type("cpu","socket,family","base_mhz,boost_mhz,l3_kb,base_w,max_w","cores,threads","igpu","socket,cores,base_w,igpu","memory,benchmarks,cpu_board",false);
        type("board","socket,chipset,form_factor,memory_type,power_connectors,pcie,storage_interfaces","memory_max_gb","memory_slots","","socket,chipset,form_factor,memory_type,power_connectors","bios,cpu_board",false);
        type("ram","memory_type,form_factor,profiles","capacity_gb,speed_mhz,height_mm,voltage_v","modules","","memory_type,form_factor,capacity_gb,modules","",false);
        type("gpu","interface,power_connectors,video_outputs","vram_gb,length_mm,height_mm,thickness_mm,slots,board_w","","","interface,length_mm,thickness_mm,board_w,power_connectors","",false);
        type("cooling","cooler_type,sockets,connectors","height_mm,radiator_mm,radiator_thickness_mm,rated_w","","","cooler_type,sockets,height_mm","",false);
        type("psu","form_factor,atx_version,efficiency,modularity","continuous_w,length_mm","","","continuous_w,form_factor,length_mm","connectors",false);
        type("case","board_formats,psu_formats,radiator_positions,drive_bays,front_connectors","gpu_max_mm,cooler_max_mm,psu_max_mm","","","board_formats,psu_formats,gpu_max_mm,cooler_max_mm","",false);
        type("storage","drive_type,interface,form_factor,m2_key","capacity_gb,power_w","","","drive_type,interface,form_factor","",false);
        type("peripheral","peripheral_type,connection,os_support,requirements","power_w","","","","",true);
        type("other","product_type,platform,requirements","","","","","",true);
        group("memory","type,configuration","speed_mhz","","","type,speed_mhz,source_url");
        group("benchmarks","application,unit,environment","score","","","application,unit,environment,score,source_url");
        group("bios","revision,version","","","date","revision,version,date,source_url");
        group("cpu_board","cpu_sku,board_sku,revision,minimum_bios","","","","cpu_sku,board_sku,revision,minimum_bios,source_url");
        group("connectors","connector","","quantity","","connector,quantity");
    }
    private final ObjectMapper mapper;
    public TechnicalDraftValidator(ObjectMapper mapper) { this.mapper=mapper; }
    private static Set<String> keys(String csv) { return csv.isBlank()?Set.of():new LinkedHashSet<>(Arrays.asList(csv.split(","))); }
    private static Map<String,Kind> fields(String text,String number,String integer,String bool,String date) {
        Map<String,Kind> map=new LinkedHashMap<>();
        for(String key:keys(text)) map.put(key,Kind.TEXT);
        for(String key:keys(number)) map.put(key,Kind.NUMBER);
        for(String key:keys(integer)) map.put(key,Kind.INTEGER);
        for(String key:keys(bool)) map.put(key,Kind.BOOLEAN);
        for(String key:keys(date)) map.put(key,Kind.DATE);
        return map;
    }
    private static void type(String id,String text,String number,String integer,String bool,String required,String groups,boolean catalog) {
        TYPES.put(id,new Type(fields(text,number,integer,bool,""),keys(required),keys(groups),catalog));
    }
    private static void group(String id,String text,String number,String integer,String date,String required) {
        Map<String,Kind> map=fields(text,number,integer,"",date);
        if(!id.equals("connectors")) map.put("source_url",Kind.URL);
        GROUPS.put(id,map);REQUIRED_GROUPS.put(id,keys(required));
    }
    private static boolean empty(JsonNode n) { return n==null||n.isNull()||(n.isTextual()&&n.asText().isBlank()); }
    private static void object(JsonNode node,String path,List<String> errors) { if(node==null||!node.isObject())errors.add(path+": se necesita un objeto JSON."); }
    private static void allowed(JsonNode node,Set<String> keys,String path,List<String> errors) {
        if(node!=null&&node.isObject())node.fieldNames().forEachRemaining(key->{if(!keys.contains(key))errors.add(path+": campo no permitido "+key+".");});
    }
    private static void text(JsonNode node,String path,int max,boolean required,List<String> errors) {
        if(empty(node)){if(required)errors.add(path+": obligatorio.");return;}
        if(!node.isTextual()||node.asText().length()>max)errors.add(path+": texto de máximo "+max+" caracteres.");
    }
    private static void url(JsonNode node,String path,List<String> errors) {
        if(empty(node))return;
        try {URI uri=URI.create(node.textValue());if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||node.asText().length()>2000)throw new IllegalArgumentException();}
        catch(Exception ex){errors.add(path+": URL HTTPS sin credenciales requerida.");}
    }
    private static void date(JsonNode node,String path,List<String> errors) {
        if(empty(node))return;
        try{if(!node.isTextual()||!node.asText().matches("\\d{4}-\\d{2}-\\d{2}")||LocalDate.parse(node.asText()).isAfter(LocalDate.now(ZoneId.of("America/Lima"))))throw new IllegalArgumentException();}
        catch(Exception ex){errors.add(path+": fecha inválida o futura.");}
    }
    private static void field(JsonNode node,Kind kind,String path,List<String> errors) {
        if(empty(node))return;
        switch(kind){
            case TEXT -> text(node,path,2000,false,errors);
            case NUMBER -> {if(!node.isNumber()||!Double.isFinite(node.asDouble())||node.decimalValue().signum()<0)errors.add(path+": número no negativo requerido.");}
            case INTEGER -> {if(!node.isIntegralNumber()||!node.canConvertToInt()||node.intValue()<1)errors.add(path+": entero positivo requerido.");}
            case BOOLEAN -> {if(!node.isBoolean())errors.add(path+": booleano o null requerido.");}
            case DATE -> date(node,path,errors);
            case URL -> url(node,path,errors);
        }
    }
    public TechnicalDraftDtos.Validation validate(JsonNode input,boolean submission) {
        List<String> errors=new ArrayList<>(),warnings=new ArrayList<>();
        object(input,"document",errors);
        if(!errors.isEmpty())return new TechnicalDraftDtos.Validation(false,errors,warnings,null);
        try{if(mapper.writeValueAsBytes(input).length>131072)throw new DraftFault(413,"El documento supera 128 KiB.");}
        catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new DraftFault(400,"Documento JSON inválido.");}
        ObjectNode doc=input.deepCopy();
        allowed(doc,keys("schema,verificationStatus,requestedUse,product,technical,relatedRecords,evidence,media,catalogProductId"),"document",errors);
        if(!empty(doc.get("catalogProductId"))){
            try{String id=doc.get("catalogProductId").textValue();if(id==null||!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException();}
            catch(Exception ex){errors.add("catalogProductId: UUID canónico requerido.");}
        }
        if(!"rya.technical-draft.v1".equals(doc.path("schema").asText()))errors.add("schema: versión no admitida.");
        doc.put("verificationStatus","pending");
        String use=doc.path("requestedUse").asText();
        if(!Set.of("catalog","pending","evaluate").contains(use))errors.add("requestedUse: valor inválido.");
        if(submission&&use.equals("pending"))errors.add("Clasifica el uso antes de enviar a revisión.");
        JsonNode product=doc.path("product");object(product,"product",errors);
        allowed(product,keys("name,sku,mpn,brand,price,stock,type,categoryProposal"),"product",errors);
        text(product.get("name"),"product.name",220,true,errors);text(product.get("sku"),"product.sku",80,true,errors);
        text(product.get("brand"),"product.brand",130,true,errors);text(product.get("mpn"),"product.mpn",120,false,errors);
        JsonNode price=product.get("price"),stock=product.get("stock");
        if(!empty(price)&&(!price.isNumber()||price.decimalValue().signum()<0||price.decimalValue().stripTrailingZeros().scale()>2||price.decimalValue().precision()-price.decimalValue().scale()>12))errors.add("product.price: importe válido de hasta dos decimales requerido.");
        if(!empty(stock)&&(!stock.isIntegralNumber()||!stock.canConvertToInt()||stock.intValue()<0))errors.add("product.stock: entero no negativo requerido.");
        Type type=TYPES.get(product.path("type").asText());
        if(type==null)errors.add("product.type: tipo no admitido.");
        if(type!=null&&type.catalogOnly&&use.equals("evaluate"))errors.add("Este tipo solo se gestiona para catálogo, no como componente de PC.");
        JsonNode category=product.get("categoryProposal");
        if(!empty(category)){
            object(category,"categoryProposal",errors);allowed(category,keys("name,slug,section,parentSlug"),"categoryProposal",errors);
            text(category.get("name"),"categoryProposal.name",120,true,errors);
            for(String key:List.of("slug","parentSlug")){JsonNode node=category.get(key);if(!empty(node)&&(!node.isTextual()||!node.asText().matches("[a-z0-9]+(?:-[a-z0-9]+)*")||node.asText().length()>240))errors.add("categoryProposal."+key+": slug inválido.");}
            if(!Set.of("hardware","software").contains(category.path("section").asText()))errors.add("categoryProposal.section: inválida.");
            if(!empty(category.get("parentSlug"))&&category.path("parentSlug").equals(category.path("slug")))errors.add("La categoría no puede ser su propia superior.");
        }
        JsonNode evidence=doc.path("evidence");object(evidence,"evidence",errors);allowed(evidence,keys("url,date,notes"),"evidence",errors);
        url(evidence.get("url"),"evidence.url",errors);date(evidence.get("date"),"evidence.date",errors);text(evidence.get("notes"),"evidence.notes",5000,false,errors);
        if(submission&&(empty(evidence.get("url"))||empty(evidence.get("date"))))errors.add("Se necesita fuente y fecha para enviar a revisión.");
        JsonNode technical=doc.path("technical"),related=doc.path("relatedRecords");object(technical,"technical",errors);object(related,"relatedRecords",errors);
        if(type!=null){
            allowed(technical,type.fields.keySet(),"technical",errors);allowed(related,type.groups,"relatedRecords",errors);
            type.fields.forEach((key,kind)->{field(technical.get(key),kind,"technical."+key,errors);if(type.required.contains(key)&&empty(technical.get(key)))(submission&&use.equals("evaluate")?errors:warnings).add("Falta technical."+key+".");});
            for(String[] pair:List.of(new String[]{"base_mhz","boost_mhz"},new String[]{"base_w","max_w"},new String[]{"cores","threads"})){
                JsonNode a=technical.get(pair[0]),b=technical.get(pair[1]);if(a!=null&&b!=null&&a.isNumber()&&b.isNumber()&&a.decimalValue().compareTo(b.decimalValue())>0)errors.add("Conflicto entre "+pair[0]+" y "+pair[1]+".");
            }
            for(String group:type.groups){JsonNode rows=related.get(group);if(rows==null)continue;
                if(!rows.isArray()||rows.size()>20){errors.add(group+": máximo 20 registros.");continue;}
                for(JsonNode row:rows){object(row,group,errors);Set<String> permitted=new HashSet<>(GROUPS.get(group).keySet());permitted.add("resolutionStatus");allowed(row,permitted,group,errors);
                    if(row.isObject()&&GROUPS.get(group).keySet().stream().allMatch(key->empty(row.get(key))))errors.add(group+": elimina los registros completamente vacíos.");
                    GROUPS.get(group).forEach((key,kind)->{field(row.get(key),kind,group+"."+key,errors);if(REQUIRED_GROUPS.get(group).contains(key)&&empty(row.get(key)))(submission&&use.equals("evaluate")?errors:warnings).add("Falta "+group+"."+key+".");});
                    if(group.equals("cpu_board")){if(!empty(row.get("cpu_sku"))&&row.path("cpu_sku").equals(row.path("board_sku")))errors.add("CPU y placa deben ser productos diferentes.");warnings.add("Relación CPU–placa sin resolver: requiere vinculación y revisión humana.");}
                    if(row.isObject())((ObjectNode)row).put("resolutionStatus","pending");
                }
            }
        }
        JsonNode media=doc.path("media");
        if(!media.isArray()||media.size()>8)errors.add("media: máximo ocho imágenes.");
        else{Set<String> urls=new HashSet<>();for(int index=0;index<media.size();index++){
            JsonNode image=media.get(index);object(image,"media",errors);allowed(image,keys("url,alt,order,primary,file"),"media",errors);
            text(image.get("alt"),"media.alt",300,false,errors);
            if(empty(image.get("url")))errors.add("Los archivos locales deben subirse antes de persistir el borrador; no se aceptan metadatos sin URL.");
            else{url(image.get("url"),"media.url",errors);String value=image.path("url").asText().split("#",2)[0];if(!urls.add(value))errors.add("Imagen duplicada.");}
            if(image.path("order").asInt(-1)!=index+1||!image.path("order").isIntegralNumber())errors.add("El orden de imágenes debe ser consecutivo desde uno.");
            if(!image.path("primary").isBoolean()||image.path("primary").asBoolean()!=(index==0))errors.add("Solo la primera imagen debe ser principal.");
            // File bytes and untrusted upload metadata never become storage ownership or deletion authority.
            if(image.isObject())((ObjectNode)image).putNull("file");
        }}
        warnings.add("Revisión local de estructura: no acredita fuentes oficiales, compatibilidad ni publicación.");
        if(errors.isEmpty()){
            // Canonical unknown values remain null, never zero/false or arbitrary empty strings.
            for(String key:List.of("price","stock"))if(empty(product.get(key)))((ObjectNode)product).putNull(key);
            if(type!=null)for(String key:type.fields.keySet())if(empty(technical.get(key)))((ObjectNode)technical).putNull(key);
            for(String key:List.of("url","date"))if(empty(evidence.get(key)))((ObjectNode)evidence).putNull(key);
        }
        return new TechnicalDraftDtos.Validation(errors.isEmpty(),List.copyOf(errors),List.copyOf(warnings),errors.isEmpty()?doc:null);
    }
}
