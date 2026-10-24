package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.FichaProductoDto;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.*;
import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.util.*;

/** Called inside the product service transaction. Uses isolated EAV codes, not certified component tables. */
public class ProductTechnicalData {
    private static final String PREFIX="CATALOG_TECH_";
    public record Field(String key, String label, String type, String unit, boolean integer, boolean positive) {}
    public record Schema(String tipo, String nombre, List<Field> campos) {}
    private static Field text(String key,String label){return new Field(key,label,"text",null,false,false);}
    private static Field number(String key,String label,String unit){return new Field(key,label,"number",unit,false,false);}
    private static Field integer(String key,String label,String unit){return new Field(key,label,"number",unit,true,true);}
    private static Field bool(String key,String label){return new Field(key,label,"boolean",null,false,false);}
    private static final Map<String,Schema> SCHEMAS=Map.of(
        "cpu",new Schema("cpu","Procesador",List.of(text("socket","Socket"),integer("cores","Núcleos",null),integer("threads","Hilos",null),number("base_mhz","Frecuencia base","MHz"),number("boost_mhz","Frecuencia máxima","MHz"),number("l3_mb","Caché L3","MB"),number("tdp_w","TDP declarado (no consumo máximo)","W"),number("base_w","Potencia base publicada","W"),number("max_w","Potencia máxima publicada","W"),text("power_notes","Criterio de potencia: TDP / PBP / PPT / MTP"),bool("igpu","Gráficos integrados"),text("memory_type","Tipo de RAM admitido"))),
        "board",new Schema("board","Placa madre",List.of(text("socket","Socket"),text("chipset","Chipset"),text("form_factor","Formato"),text("memory_type","Tipo de RAM"),integer("memory_slots","Ranuras de RAM",null),number("memory_max_gb","RAM máxima","GB"),text("power_connectors","Conectores de alimentación"),text("pcie","Ranuras PCIe y versiones"),text("storage_interfaces","Interfaces M.2 / SATA"),text("revision","Revisión de hardware"),text("bios","BIOS indicada (no certifica CPU)"),number("estimated_w","Consumo estimado, no certificado","W"),number("m2_slots","Ranuras M.2",null),number("sata_ports","Puertos SATA",null),bool("wifi","Wi-Fi integrado"),bool("bluetooth","Bluetooth integrado"),number("ethernet_mbps","Velocidad Ethernet","Mbps"),number("memory_oc_mts","Velocidad RAM máxima OC publicada","MT/s"))),
        "ram",new Schema("ram","Memoria RAM",List.of(text("memory_type","Tipo DDR"),text("form_factor","Formato DIMM / SO-DIMM"),number("capacity_gb","Capacidad total del kit","GB"),integer("modules","Módulos por kit",null),number("speed_mts","Velocidad probada","MT/s"),number("spd_mts","Velocidad SPD","MT/s"),integer("cas","Latencia CAS (CL)",null),number("voltage_v","Voltaje probado","V"),text("profiles","Perfiles JEDEC / XMP / EXPO"),number("height_mm","Altura del módulo","mm"),number("power_w","Consumo publicado","W"))),
        "storage",new Schema("storage","Almacenamiento",List.of(text("drive_type","SSD / HDD"),number("capacity_gb","Capacidad","GB"),text("interface","Interfaz y versión SATA / PCIe"),text("form_factor","Formato: 2.5, 3.5, M.2 2280…"),text("m2_key","Clave M.2"),number("read_mbs","Lectura secuencial publicada","MB/s"),number("write_mbs","Escritura secuencial publicada","MB/s"),number("power_w","Consumo publicado","W"))),
        "gpu",new Schema("gpu","Tarjeta gráfica",List.of(text("interface","Interfaz PCIe"),number("vram_gb","Memoria de video","GB"),text("memory_type","Tipo de VRAM"),number("length_mm","Largo","mm"),number("height_mm","Alto","mm"),number("thickness_mm","Grosor","mm"),number("slots","Ranuras ocupadas",null),number("board_w","Potencia de tarjeta publicada","W"),text("power_notes","Criterio de potencia y condiciones"),number("recommended_psu_w","Fuente recomendada por fabricante","W"),text("power_connectors","Conectores de alimentación"),text("video_outputs","Salidas de video"))),
        "psu",new Schema("psu","Fuente de poder",List.of(number("continuous_w","Potencia de salida continua (no consumo)","W"),text("form_factor","Formato ATX / SFX"),text("atx_version","Versión ATX"),text("efficiency","Certificación de eficiencia"),text("modularity","Modularidad"),text("connectors","Tipos y cantidades de conectores"),number("length_mm","Largo","mm"))),
        "keyboard",new Schema("keyboard","Teclado (sin validación CPU–placa)",List.of(text("color","Color"),text("layout","Distribución regional"),text("connections","Conexiones"),bool("wireless","Inalámbrico"),bool("backlight","Retroiluminación"),integer("devices","Dispositivos enlazados",null),text("systems","Sistemas indicados"),text("variant_notes","Variante y limitaciones"))),
        "mouse",new Schema("mouse","Mouse (sin validación CPU–placa)",List.of(text("edition","Edición"),text("color","Color"),text("sensor","Tipo de sensor"),number("max_dpi","Resolución máxima","DPI"),text("wheel","Rueda"),bool("silent_clicks","Clics silenciosos"),text("connections","Conexiones"),text("variant_notes","Variante y contenido de caja")))
    );
    private static final Map<String,String> ALIASES=Map.ofEntries(
        Map.entry("procesadores","cpu"),Map.entry("procesador","cpu"),Map.entry("cpu","cpu"),
        Map.entry("placasmadre","board"),Map.entry("placamadre","board"),Map.entry("placasbase","board"),Map.entry("motherboards","board"),
        Map.entry("memoriasram","ram"),Map.entry("memoriaram","ram"),Map.entry("ram","ram"),
        Map.entry("almacenamiento","storage"),Map.entry("ssd","storage"),Map.entry("hdd","storage"),
        Map.entry("tarjetasgraficas","gpu"),Map.entry("tarjetagrafica","gpu"),Map.entry("gpu","gpu"),
        Map.entry("fuentesdepoder","psu"),Map.entry("fuentedepoder","psu"),Map.entry("fuentesdealimentacion","psu"),Map.entry("psu","psu"),
        Map.entry("teclados","keyboard"),Map.entry("teclado","keyboard"),Map.entry("mouse","mouse"),Map.entry("ratones","mouse")
    );
    private static String key(String raw){return Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    public static Schema schema(Categoria category,CategoriaRepository categories){
        var visited=new HashSet<UUID>();
        for(int depth=0;category!=null && depth<20;depth++){
            String type=ALIASES.getOrDefault(key(category.getSlug()),ALIASES.get(key(category.getNombre())));
            if(type!=null)return SCHEMAS.get(type);
            if(category.getParentId()==null||!visited.add(category.getParentId()))return null;
            category=categories.findById(category.getParentId()).filter(c->!Boolean.TRUE.equals(c.getIsDeleted())).orElse(null);
        }
        return null;
    }
    private final AtributoTecnicoRepository attributes;
    private final ValorAtributoProductoRepository values;
    private final CategoriaRepository categories;
    private final java.util.function.Consumer<ValorAtributoProducto> restorer;
    public ProductTechnicalData(AtributoTecnicoRepository attributes,ValorAtributoProductoRepository values,CategoriaRepository categories){
        this(attributes,values,categories,row->{throw new com.ecommerce.backend.exception.ConflictException("Este campo requiere restauración auditada antes de guardarse.");});
    }
    public ProductTechnicalData(AtributoTecnicoRepository attributes,ValorAtributoProductoRepository values,CategoriaRepository categories,java.util.function.Consumer<ValorAtributoProducto> restorer){this.attributes=attributes;this.values=values;this.categories=categories;this.restorer=restorer;}

    public void save(Producto product,FichaProductoDto dto){
        if(dto==null)return;
        var schema=schema(product.getCategoria(),categories);
        if(schema==null||!schema.tipo().equals(dto.tipo()))throw new BadRequestException("La ficha técnica no corresponde a la categoría seleccionada.");
        if(dto.valores()==null)throw new BadRequestException("Los valores de la ficha deben ser un objeto.");
        var definitions=new LinkedHashMap<String,Field>();var normalized=new LinkedHashMap<String,Object>();
        schema.campos().forEach(field->definitions.put(field.key(),field));
        dto.valores().forEach((id,raw)->{
            var field=definitions.get(id);if(field==null)throw new BadRequestException("Campo técnico desconocido para esta categoría: "+id);
            Object value=normalize(field,raw);if(value!=null)normalized.put(id,value);
        });
        String source=dto.fuenteUrl()==null?"":dto.fuenteUrl().trim();
        if(!source.isEmpty()){
            try{var url=URI.create(source);if(source.length()>250||!"https".equalsIgnoreCase(url.getScheme())||url.getHost()==null||url.getUserInfo()!=null)throw new IllegalArgumentException();}
            catch(IllegalArgumentException ex){throw new BadRequestException("La fuente técnica debe ser una URL HTTPS sin credenciales, de hasta 250 caracteres.");}
        }
        var existing=values.findAllByProducto_Id(product.getId());
        var expected=new HashSet<String>();
        write(product,existing,PREFIX+"TYPE",text("type","Familia técnica"),schema.tipo());expected.add(PREFIX+"TYPE");
        write(product,existing,PREFIX+"STATUS",text("status","Estado de revisión técnica"),"Pendiente de revisión; no certifica compatibilidad");expected.add(PREFIX+"STATUS");
        if(!source.isEmpty()){write(product,existing,PREFIX+"SOURCE",text("source","Fuente de especificaciones"),source);expected.add(PREFIX+"SOURCE");}
        normalized.forEach((id,value)->{String code=PREFIX+schema.tipo().toUpperCase(Locale.ROOT)+"_"+id.toUpperCase(Locale.ROOT);write(product,existing,code,definitions.get(id),value);expected.add(code);});
        for(var old:existing)if(old.getAtributo()!=null&&old.getAtributo().getCodigo().startsWith(PREFIX)&&!expected.contains(old.getAtributo().getCodigo())){old.setIsDeleted(true);values.save(old);}
    }
    private Object normalize(Field field,Object raw){
        if(raw==null||raw instanceof String s&&s.isBlank())return null;
        if("boolean".equals(field.type())){if(raw instanceof Boolean)return raw;throw new BadRequestException(field.label()+": selecciona Sí, No o desconocido.");}
        if("number".equals(field.type())){
            if(!(raw instanceof Number))throw new BadRequestException(field.label()+": se requiere un número, no texto.");
            try{var number=new BigDecimal(raw.toString()).stripTrailingZeros();
                if(number.signum()<0||field.positive()&&number.signum()==0||number.scale()>4||number.precision()-number.scale()>12||field.integer()&&number.scale()>0)throw new NumberFormatException();return number;
            }catch(NumberFormatException ex){throw new BadRequestException(field.label()+": número no negativo válido, máximo cuatro decimales; los conteos deben ser enteros positivos.");}
        }
        if(!(raw instanceof String))throw new BadRequestException(field.label()+": se requiere texto.");
        String result=((String)raw).trim();if(result.length()>250)throw new BadRequestException(field.label()+": máximo 250 caracteres.");return result;
    }
    private void write(Producto product,List<ValorAtributoProducto> existing,String code,Field field,Object value){
        String dataType="number".equals(field.type())?"NUMERO":"boolean".equals(field.type())?"BOOLEANO":"TEXTO";
        var attribute=attributes.findByCodigo(code).orElseGet(()->attributes.save(AtributoTecnico.builder().codigo(code).nombre("Ficha · "+field.label()).tipoDato(dataType).unidad(field.unit()).filtrable(false).isDeleted(false).build()));
        if(Boolean.TRUE.equals(attribute.getIsDeleted())||!dataType.equals(attribute.getTipoDato())||!Objects.equals(field.unit(),attribute.getUnidad()))throw new BadRequestException("La definición técnica "+code+" requiere revisión administrativa.");
        var row=existing.stream().filter(v->v.getAtributo()!=null&&code.equals(v.getAtributo().getCodigo())).findFirst().orElseGet(()->ValorAtributoProducto.builder().producto(product).atributo(attribute).build());
        if(Boolean.TRUE.equals(row.getIsDeleted()))restorer.accept(row);
        row.setIsDeleted(false);row.setValorTexto(null);row.setValorNumero(null);row.setValorBooleano(null);
        if(value instanceof BigDecimal number)row.setValorNumero(number);else if(value instanceof Boolean bool)row.setValorBooleano(bool);else row.setValorTexto((String)value);
        values.save(row);
    }
    public void clear(Producto product){for(var row:values.findAllByProducto_Id(product.getId()))if(row.getAtributo()!=null&&row.getAtributo().getCodigo().startsWith(PREFIX)){row.setIsDeleted(true);values.save(row);}}
    public static FichaProductoDto read(List<ValorAtributoProducto> rows){
        String type=null,source=null;var result=new LinkedHashMap<String,Object>();
        for(var row:rows){if(Boolean.TRUE.equals(row.getIsDeleted())||row.getAtributo()==null)continue;String code=row.getAtributo().getCodigo();if((PREFIX+"TYPE").equals(code))type=row.getValorTexto();if((PREFIX+"SOURCE").equals(code))source=row.getValorTexto();}
        if(!SCHEMAS.containsKey(type==null?"":type))return null;
        String fieldPrefix=PREFIX+type.toUpperCase(Locale.ROOT)+"_";
        for(var row:rows){if(Boolean.TRUE.equals(row.getIsDeleted())||row.getAtributo()==null)continue;String code=row.getAtributo().getCodigo();if(code.startsWith(fieldPrefix))result.put(code.substring(fieldPrefix.length()).toLowerCase(Locale.ROOT),row.getValorNumero()!=null?row.getValorNumero():row.getValorBooleano()!=null?row.getValorBooleano():row.getValorTexto());}
        return new FichaProductoDto(type,source,result);
    }
}
