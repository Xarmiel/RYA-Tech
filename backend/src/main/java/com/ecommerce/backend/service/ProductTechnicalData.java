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
    public static boolean isInternalAttributeCode(String code) {
        return code!=null && code.startsWith(PREFIX) && (code.endsWith("_INTERNAL_NOTES") || code.endsWith("_POWER_NOTES"));
    }
    /** Public commercial facets do not certify technical compatibility. Metadata and free-form notes stay private to the sheet. */
    public static boolean isPublicFilterCode(String code) {
        if (code == null) return false;
        var excluded = Set.of("power_notes", "variant_notes", "connectors", "power_connectors", "pcie", "storage_interfaces", "video_outputs",
            "sensitivity", "microphone_type", "video_modes", "refresh_modes", "response_metric", "power_requirements",
            "measurement_conditions", "compatible_accessories", "radiator_dimensions");
        return SCHEMAS.values().stream().anyMatch(schema -> schema.campos().stream().anyMatch(field ->
            !excluded.contains(field.key()) && (PREFIX + schema.tipo().toUpperCase(Locale.ROOT) + "_" + field.key().toUpperCase(Locale.ROOT)).equals(code)));
    }
    public record Field(String key, String label, String type, String unit, boolean integer, boolean positive, String example, boolean custom) {
        public Field(String key,String label,String type,String unit,boolean integer,boolean positive){this(key,label,type,unit,integer,positive,null,false);}
    }
    public record Schema(String tipo, String nombre, List<Field> campos) {}
    private static Field text(String key,String label){return new Field(key,label,"text",null,false,false);}
    private static Field number(String key,String label,String unit){return new Field(key,label,"number",unit,false,false);}
    private static Field integer(String key,String label,String unit){return new Field(key,label,"number",unit,true,true);}
    private static Field count(String key,String label){return new Field(key,label,"number",null,true,false);}
    private static Field bool(String key,String label){return new Field(key,label,"boolean",null,false,false);}
    private static final Map<String,Schema> SCHEMAS=Map.ofEntries(
        Map.entry("cpu",new Schema("cpu","Procesador",List.of(text("socket","Socket"),text("generacion","Generación"),integer("cores","Núcleos",null),integer("threads","Hilos",null),number("base_mhz","Frecuencia base","MHz"),number("boost_mhz","Frecuencia máxima","MHz"),number("l3_mb","Caché L3","MB"),number("tdp_w","TDP declarado (no consumo máximo)","W"),number("base_w","Potencia base publicada","W"),number("max_w","Potencia máxima publicada","W"),text("power_notes","Criterio de potencia: TDP / PBP / PPT / MTP"),bool("igpu","Gráficos integrados"),text("memory_type","Tipo de RAM admitido"),bool("cooler_included","Disipador incluido")))),
        Map.entry("board",new Schema("board","Placa madre",List.of(text("socket","Socket"),text("chipset","Chipset"),text("form_factor","Formato"),text("memory_type","Tipo de RAM"),integer("memory_slots","Ranuras de RAM",null),number("memory_max_gb","RAM máxima","GB"),text("power_connectors","Conectores de alimentación"),text("pcie","Ranuras PCIe y versiones"),text("storage_interfaces","Interfaces M.2 / SATA"),text("m2_pcie_version","Versión PCIe M.2"),text("revision","Revisión de hardware"),number("estimated_w","Consumo estimado, no certificado","W"),number("m2_slots","Ranuras M.2",null),number("sata_ports","Puertos SATA",null),bool("wifi","Wi-Fi integrado"),bool("bluetooth","Bluetooth integrado"),number("ethernet_mbps","Velocidad Ethernet","Mbps"),number("memory_oc_mts","Velocidad RAM máxima OC publicada","MT/s")))),
        Map.entry("ram",new Schema("ram","Memoria RAM",List.of(text("memory_type","Tipo DDR"),text("form_factor","Formato DIMM / SO-DIMM"),number("capacity_gb","Capacidad total del kit","GB"),integer("modules","Módulos por kit",null),number("speed_mts","Velocidad probada","MT/s"),number("spd_mts","Velocidad SPD","MT/s"),integer("cas","Latencia CAS (CL)",null),number("voltage_v","Voltaje probado","V"),text("profiles","Perfiles JEDEC / XMP / EXPO"),number("height_mm","Altura del módulo","mm"),number("power_w","Consumo publicado","W")))),
        Map.entry("storage",new Schema("storage","Almacenamiento",List.of(text("drive_type","SSD / HDD"),number("capacity_gb","Capacidad","GB"),text("interface","Interfaz y versión SATA / PCIe"),text("form_factor","Formato: 2.5, 3.5, M.2 2280…"),text("m2_key","Clave M.2"),number("read_mbs","Lectura secuencial publicada","MB/s"),number("write_mbs","Escritura secuencial publicada","MB/s"),number("power_w","Consumo publicado","W")))),
        Map.entry("gpu",new Schema("gpu","Tarjeta gráfica",List.of(text("interface","Interfaz PCIe"),number("vram_gb","Memoria de video","GB"),text("memory_type","Tipo de VRAM"),number("length_mm","Largo","mm"),number("height_mm","Alto","mm"),number("thickness_mm","Grosor","mm"),number("slots","Ranuras ocupadas",null),number("board_w","Potencia de tarjeta publicada","W"),text("power_notes","Criterio de potencia y condiciones"),number("recommended_psu_w","Fuente recomendada por fabricante","W"),text("power_connectors","Conectores de alimentación"),text("video_outputs","Salidas de video")))),
        Map.entry("psu",new Schema("psu","Fuente de poder",List.of(number("continuous_w","Potencia de salida continua (no consumo)","W"),text("form_factor","Formato ATX / SFX"),text("atx_version","Versión ATX"),text("efficiency","Certificación de eficiencia"),text("modularity","Modularidad"),text("connectors","Tipos y cantidades de conectores"),number("length_mm","Largo","mm")))),
        Map.entry("case",new Schema("case","Gabinete",List.of(text("form_factor","Factor de forma de la placa"),number("height_mm","Altura","mm"),number("length_mm","Longitud","mm"),number("width_mm","Ancho","mm"),number("psu_max_length_mm","Profundidad de fuente","mm"),number("gpu_max_length_mm","Longitud de la GPU","mm"),number("cooler_max_height_mm","Altura del disipador","mm"),integer("max_fans","Máximo ventiladores",null)))),
        Map.entry("keyboard",new Schema("keyboard","Teclado (sin validación CPU–placa)",List.of(text("color","Color"),text("layout","Distribución regional"),text("connections","Conexiones"),bool("wireless","Inalámbrico"),bool("backlight","Retroiluminación"),integer("devices","Dispositivos enlazados",null),text("systems","Sistemas indicados"),text("variant_notes","Variante y limitaciones"),text("switch_type","Tipo y modelo de switches"),text("switch_feel","Mecanismo y sensación del switch"),text("form_factor","Formato: completo, TKL, 75 %, 60 % u otro"),bool("hot_swap","Switches intercambiables (hot-swap)"),bool("programmable_keys","Teclas programables"),number("polling_hz","Tasa de sondeo máxima","Hz"),number("weight_g","Peso publicado","g"),number("battery_hours","Autonomía publicada","h")))),
        Map.entry("mouse",new Schema("mouse","Mouse (sin validación CPU–placa)",List.of(text("edition","Edición"),text("color","Color"),text("sensor","Tipo de sensor"),number("max_dpi","Resolución máxima","DPI"),text("wheel","Rueda"),bool("silent_clicks","Clics silenciosos"),text("connections","Conexiones"),text("variant_notes","Variante y contenido de caja"),number("weight_g","Peso publicado","g"),count("buttons","Botones declarados"),bool("programmable_buttons","Botones programables"),number("polling_hz","Tasa de sondeo máxima","Hz"),number("battery_hours","Autonomía publicada","h"),text("grip","Forma y agarre indicado"),text("systems","Sistemas admitidos")))),
        Map.entry("headset", new Schema("headset","Audífonos y auriculares",List.of(
            text("design","Diseño acústico: abierto, cerrado o intraaural"),
            text("fit","Formato y colocación"),
            text("driver_type","Tipo de transductor"),
            number("driver_mm","Diámetro del transductor","mm"),
            number("frequency_min_hz","Frecuencia mínima publicada","Hz"),
            number("frequency_max_hz","Frecuencia máxima publicada","Hz"),
            number("impedance_ohm","Impedancia","Ω"),
            text("sensitivity","Sensibilidad con unidad y referencia de medición"),
            text("connections","Conexiones y conectores"),
            bool("wireless","Inalámbrico"),
            bool("microphone","Micrófono integrado"),
            text("microphone_type","Tipo y características del micrófono"),
            bool("anc","Cancelación activa de ruido (ANC)"),
            text("audio_formats","Formatos de audio declarados"),
            number("battery_hours","Autonomía publicada","h"),
            number("cable_length_m","Longitud del cable","m"),
            number("weight_g","Peso publicado","g"),
            text("platforms","Plataformas indicadas por el fabricante"),
            text("variant_notes","Variante y accesorios incluidos")))),
        Map.entry("microphone", new Schema("microphone","Micrófono",List.of(
            text("microphone_type","Tipo: dinámico, condensador u otro"),
            text("polar_pattern","Patrones polares disponibles"),
            text("connections","Conexiones USB / XLR y conectores"),
            number("frequency_min_hz","Frecuencia mínima publicada","Hz"),
            number("frequency_max_hz","Frecuencia máxima publicada","Hz"),
            text("sensitivity","Sensibilidad con unidad y referencia (puede ser negativa)"),
            number("impedance_ohm","Impedancia de salida","Ω"),
            number("max_spl_db","Presión sonora máxima publicada","dB SPL"),
            number("sample_rate_khz","Frecuencia de muestreo máxima","kHz"),
            integer("bit_depth","Profundidad de bits","bits"),
            bool("phantom_power","Requiere alimentación phantom"),
            text("power_requirements","Alimentación y voltajes admitidos"),
            bool("headphone_output","Salida para auriculares"),
            bool("mute","Control de silencio"),
            text("mount","Montaje y rosca"),
            number("weight_g","Peso publicado","g"),
            text("variant_notes","Interfaz requerida y accesorios incluidos")))),
        Map.entry("webcam", new Schema("webcam","Cámara web",List.of(
            text("max_resolution","Resolución máxima publicada"),
            number("max_fps","FPS máximos (consultar modos)","fps"),
            text("video_modes","Modos de resolución, FPS y HDR correspondientes"),
            text("sensor","Sensor declarado"),
            text("focus","Tipo de enfoque"),
            number("field_of_view_deg","Campo de visión publicado","°"),
            text("connections","Conexiones y requisitos USB"),
            bool("microphone","Micrófono integrado"),
            bool("hdr","HDR disponible (consultar modos)"),
            bool("privacy_shutter","Cubierta de privacidad"),
            text("zoom","Zoom declarado y tipo óptico / digital"),
            text("mount","Montaje"),
            text("systems","Sistemas admitidos"),
            number("weight_g","Peso publicado","g"),
            text("variant_notes","Variante y accesorios incluidos")))),
        Map.entry("mousepad", new Schema("mousepad","Mousepad o alfombrilla",List.of(
            number("width_mm","Ancho","mm"),
            number("length_mm","Largo","mm"),
            number("thickness_mm","Grosor","mm"),
            text("material","Material"),
            text("surface","Tipo de superficie"),
            text("base","Material y propiedades de la base"),
            bool("stitched_edges","Bordes cosidos"),
            bool("water_resistant","Resistencia a derrames declarada"),
            bool("rgb","Iluminación RGB"),
            bool("wireless_charging","Carga inalámbrica"),
            text("connections","Conexiones, si requiere alimentación"),
            text("color","Color"),
            text("variant_notes","Tamaño comercial y variante")))),
        Map.entry("monitor", new Schema("monitor","Monitor",List.of(
            number("size_inches","Diagonal de pantalla","pulgadas"),
            text("resolution","Resolución nativa"),
            text("panel","Tipo de panel"),
            number("refresh_hz","Frecuencia de actualización máxima","Hz"),
            text("refresh_modes","Resolución, frecuencia y conexión correspondientes"),
            number("response_ms","Tiempo de respuesta publicado","ms"),
            text("response_metric","Métrica y condiciones: GtG, MPRT u otra"),
            number("brightness_nits","Brillo publicado","cd/m²"),
            text("contrast","Contraste declarado"),
            text("hdr","HDR o certificación declarada"),
            text("color_gamut","Cobertura de color y espacio de referencia"),
            text("adaptive_sync","Sincronización adaptativa declarada"),
            text("connections","Puertos y versiones"),
            text("vesa","Montaje VESA y dimensiones"),
            bool("height_adjustable","Altura ajustable"),
            bool("speakers","Altavoces integrados"),
            number("power_w","Consumo publicado","W"),
            text("variant_notes","Variante y cables incluidos")))),
        Map.entry("racing", new Schema("racing","Volante, pedales y accesorios de carreras",List.of(
            text("product_type","Tipo de producto: volante, base, pedales u otro"),
            text("platforms","Plataformas admitidas"),
            text("connections","Conexiones"),
            number("rotation_deg","Ángulo de giro máximo","°"),
            bool("force_feedback","Force feedback"),
            text("feedback_type","Tecnología de force feedback"),
            number("torque_nm","Par publicado por el fabricante","N·m"),
            text("pedals","Pedales incluidos y tecnología"),
            text("shifter","Palanca incluida o admitida"),
            text("mount","Sistema de montaje"),
            text("power_requirements","Alimentación"),
            text("compatible_accessories","Accesorios compatibles según fabricante"),
            text("variant_notes","Contenido de caja y variante")))),
        Map.entry("simulation", new Schema("simulation","Controles de simulación",List.of(
            text("product_type","Tipo: joystick, HOTAS, yoke, panel u otro"),
            text("platforms","Plataformas admitidas"),
            text("connections","Conexiones"),
            count("axes","Ejes declarados"),
            count("buttons","Botones declarados"),
            text("controls","Palancas, interruptores y otros controles"),
            bool("force_feedback","Force feedback declarado"),
            text("mount","Sistema de montaje"),
            text("power_requirements","Alimentación"),
            text("compatible_accessories","Accesorios y software indicados"),
            number("weight_g","Peso publicado","g"),
            text("variant_notes","Contenido de caja y variante")))),
        Map.entry("cooler", new Schema("cooler","Refrigeración",List.of(
            text("cooling_type","Tipo: aire, líquida AIO, ventilador u otro"),
            text("sockets","Sockets admitidos por fabricante"),
            number("height_mm","Altura del disipador","mm"),
            number("width_mm","Ancho","mm"),
            number("length_mm","Largo","mm"),
            number("radiator_mm","Formato nominal del radiador","mm"),
            text("radiator_dimensions","Dimensiones completas del radiador y ventiladores"),
            count("fan_count","Ventiladores incluidos"),
            number("fan_size_mm","Diámetro nominal del ventilador","mm"),
            number("rpm_min","Velocidad mínima publicada","RPM"),
            number("rpm_max","Velocidad máxima publicada","RPM"),
            number("noise_db","Ruido publicado y condiciones del fabricante","dBA"),
            text("connections","Conectores y control PWM / iluminación"),
            number("tdp_w","Capacidad térmica declarada (no consumo)","W"),
            number("power_w","Consumo publicado del sistema","W"),
            text("variant_notes","Anclajes, adaptadores y pasta incluidos")))),
        Map.entry("lighting", new Schema("lighting","Iluminación",List.of(
            text("product_type","Tipo: panel, aro, tira LED u otro"),
            text("power_requirements","Alimentación y voltaje admitido"),
            number("power_w","Potencia publicada","W"),
            number("temperature_min_k","Temperatura de color mínima","K"),
            number("temperature_max_k","Temperatura de color máxima","K"),
            number("brightness_lm","Flujo luminoso publicado","lm"),
            number("illuminance_lux","Iluminancia publicada","lux"),
            text("measurement_conditions","Distancia y condiciones de medición"),
            text("cri","Índice de reproducción cromática declarado"),
            bool("rgb","Color RGB"),
            bool("dimmable","Intensidad regulable"),
            text("connections","Conexiones y control"),
            text("mount","Montaje"),
            text("dimensions","Dimensiones"),
            text("variant_notes","Fuente de alimentación y accesorios incluidos"))))
    );
    private static final Map<String,String> ALIASES=Map.ofEntries(
        Map.entry("procesadores","cpu"),Map.entry("procesador","cpu"),Map.entry("cpu","cpu"),
        Map.entry("placasmadre","board"),Map.entry("placamadre","board"),Map.entry("placasbase","board"),Map.entry("motherboards","board"),
        Map.entry("memoriasram","ram"),Map.entry("memoriaram","ram"),Map.entry("ram","ram"),
        Map.entry("almacenamiento","storage"),Map.entry("ssd","storage"),Map.entry("hdd","storage"),
        Map.entry("tarjetasgraficas","gpu"),Map.entry("tarjetagrafica","gpu"),Map.entry("gpu","gpu"),
        Map.entry("fuentesdepoder","psu"),Map.entry("fuentedepoder","psu"),Map.entry("fuentesdealimentacion","psu"),Map.entry("psu","psu"),
        Map.entry("gabinetes","case"),Map.entry("gabinete","case"),Map.entry("cases","case"),Map.entry("case","case"),
        Map.entry("teclados","keyboard"),Map.entry("teclado","keyboard"),Map.entry("mouse","mouse"),Map.entry("ratones","mouse"),
        Map.entry("keyboard","keyboard"),
        Map.entry("keyboards","keyboard"),
        Map.entry("tecladosgaming","keyboard"),
        Map.entry("tecladosparagaming","keyboard"),
        Map.entry("mouses","mouse"),
        Map.entry("mice","mouse"),
        Map.entry("mousegaming","mouse"),
        Map.entry("mouseparagaming","mouse"),
        Map.entry("audifonosgaming","headset"),
        Map.entry("auricularesgaming","headset"),
        Map.entry("mousepadsgaming","mousepad"),
        Map.entry("volantescarreras","racing"),
        Map.entry("racing","racing"),
        Map.entry("audifonos","headset"),
        Map.entry("audifono","headset"),
        Map.entry("auriculares","headset"),
        Map.entry("auricular","headset"),
        Map.entry("headset","headset"),
        Map.entry("headsets","headset"),
        Map.entry("headphones","headset"),
        Map.entry("audio","headset"),
        Map.entry("audifonosconmicrofono","headset"),
        Map.entry("audifonosconmicrofonoparajuegos","headset"),
        Map.entry("audioaudifonos","headset"),
        Map.entry("audioyaudifonosconmicrofonoparajuegos","headset"),
        Map.entry("microfono","microphone"),
        Map.entry("microfonos","microphone"),
        Map.entry("microphones","microphone"),
        Map.entry("microfonosusb","microphone"),
        Map.entry("microfonosxlr","microphone"),
        Map.entry("webcam","webcam"),
        Map.entry("webcams","webcam"),
        Map.entry("camarasweb","webcam"),
        Map.entry("camaraweb","webcam"),
        Map.entry("camaras","webcam"),
        Map.entry("mousepad","mousepad"),
        Map.entry("mousepads","mousepad"),
        Map.entry("mousepadsparagaming","mousepad"),
        Map.entry("alfombrillas","mousepad"),
        Map.entry("alfombrilladeraton","mousepad"),
        Map.entry("monitor","monitor"),
        Map.entry("monitores","monitor"),
        Map.entry("pantallas","monitor"),
        Map.entry("volantes","racing"),
        Map.entry("volante","racing"),
        Map.entry("volantesdecarreras","racing"),
        Map.entry("pedales","racing"),
        Map.entry("volantesypedales","racing"),
        Map.entry("simulacion","simulation"),
        Map.entry("simuladores","simulation"),
        Map.entry("simulaciondevuelo","simulation"),
        Map.entry("controlesdesimulacion","simulation"),
        Map.entry("joysticks","simulation"),
        Map.entry("hotas","simulation"),
        Map.entry("cooler","cooler"),
        Map.entry("coolers","cooler"),
        Map.entry("refrigeracion","cooler"),
        Map.entry("refrigeracionliquida","cooler"),
        Map.entry("disipadores","cooler"),
        Map.entry("ventiladores","cooler"),
        Map.entry("disipador","cooler"),
        Map.entry("iluminacion","lighting"),
        Map.entry("luces","lighting"),
        Map.entry("arosdeluz","lighting"),
        Map.entry("panelesled","lighting")
    );
    private static String key(String raw){return Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    public static Schema schema(Categoria category,CategoriaRepository categories){
        if(category==null)return null;
        String name=category.getNombre(),type=null;
        var visited=new HashSet<UUID>();var extra=new LinkedHashMap<String,Field>();
        for(int depth=0;category!=null && depth<20;depth++){
            if(category.getId()!=null&&!visited.add(category.getId()))break;
            if(type==null)type=ALIASES.getOrDefault(key(category.getSlug()),ALIASES.get(key(category.getNombre())));
            if(category.getId()!=null)for(var field:categories.findTechnicalFields(category.getId()))
                extra.putIfAbsent(field.getClave(),new Field(field.getClave(),field.getNombre(),field.getTipoDato(),field.getUnidad(),field.isSoloEnteros(),false,field.getEjemplo(),true));
            if(category.getParentId()==null)break;
            category=categories.findById(category.getParentId()).filter(c->!Boolean.TRUE.equals(c.getIsDeleted())).orElse(null);
        }
        if(type==null&&extra.isEmpty())return null;
        var base=type==null?null:SCHEMAS.get(type);
        var fields=new ArrayList<Field>();if(base!=null)fields.addAll(base.campos());
        fields.addAll(extra.values());fields.add(text("internal_notes","Notas internas de revisión (no se publican)"));
        return new Schema(type==null?"custom":type,base==null?name:base.nombre(),List.copyOf(fields));
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
        validateRanges(normalized);
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
    private static void validateRanges(Map<String,Object> data){
        for(String[] pair:List.of(new String[]{"frequency_min_hz","frequency_max_hz"},new String[]{"rpm_min","rpm_max"},new String[]{"temperature_min_k","temperature_max_k"})){
            if(data.get(pair[0]) instanceof BigDecimal min && data.get(pair[1]) instanceof BigDecimal max && min.compareTo(max)>0)
                throw new BadRequestException("El valor mínimo no puede superar al máximo: "+pair[0]+" / "+pair[1]);
        }
    }
    private Object normalize(Field field,Object raw){
        if(raw==null||raw instanceof String s&&s.isBlank())return null;
        if("boolean".equals(field.type())){if(raw instanceof Boolean)return raw;throw new BadRequestException(field.label()+": selecciona Sí, No o desconocido.");}
        if("number".equals(field.type())){
            if(!(raw instanceof Number))throw new BadRequestException(field.label()+": se requiere un número, no texto.");
            try{var number=new BigDecimal(raw.toString()).stripTrailingZeros();
                if(number.signum()<0&&!field.custom()||field.positive()&&number.signum()==0||number.scale()>4||(long)number.precision()-number.scale()>12||field.integer()&&number.scale()>0)throw new NumberFormatException();return number;
            }catch(NumberFormatException ex){throw new BadRequestException(field.label()+": número válido"+(field.custom()?"":" no negativo")+", máximo cuatro decimales; los conteos deben ser enteros y respetar el mínimo indicado.");}
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
    public void retainForSchema(Producto product,Schema schema){
        var current=read(values.findAllByProducto_Id(product.getId()));
        if(current==null||schema==null||!schema.tipo().equals(current.tipo())){clear(product);return;}
        var allowed=new HashSet<String>();schema.campos().forEach(field->allowed.add(field.key()));
        var kept=new LinkedHashMap<String,Object>();current.valores().forEach((key,value)->{if(allowed.contains(key))kept.put(key,value);});
        save(product,new FichaProductoDto(current.tipo(),current.fuenteUrl(),kept));
    }
    public void clear(Producto product){for(var row:values.findAllByProducto_Id(product.getId()))if(row.getAtributo()!=null&&row.getAtributo().getCodigo().startsWith(PREFIX)){row.setIsDeleted(true);values.save(row);}}
    public static FichaProductoDto read(List<ValorAtributoProducto> rows){
        String type=null,source=null;var result=new LinkedHashMap<String,Object>();
        for(var row:rows){if(Boolean.TRUE.equals(row.getIsDeleted())||row.getAtributo()==null)continue;String code=row.getAtributo().getCodigo();if((PREFIX+"TYPE").equals(code))type=row.getValorTexto();if((PREFIX+"SOURCE").equals(code))source=row.getValorTexto();}
        if(!SCHEMAS.containsKey(type==null?"":type)&&!"custom".equals(type))return null;
        String fieldPrefix=PREFIX+type.toUpperCase(Locale.ROOT)+"_";
        for(var row:rows){if(Boolean.TRUE.equals(row.getIsDeleted())||row.getAtributo()==null)continue;String code=row.getAtributo().getCodigo();if(code.startsWith(fieldPrefix))result.put(code.substring(fieldPrefix.length()).toLowerCase(Locale.ROOT),row.getValorNumero()!=null?row.getValorNumero():row.getValorBooleano()!=null?row.getValorBooleano():row.getValorTexto());}
        return new FichaProductoDto(type,source,result);
    }
}
