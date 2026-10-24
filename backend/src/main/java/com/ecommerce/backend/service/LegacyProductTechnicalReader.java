package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.FichaProductoDto;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** Read-only adaptation. No creation, certification or changes to normalized records. */
@Component
@RequiredArgsConstructor
public class LegacyProductTechnicalReader {
    private final CategoriaRepository categories;
    private final EspecificacionCpuRepository cpus;
    private final RevisionPlacaRepository boards;
    private final JdbcTemplate jdbc;

    public FichaProductoDto read(Producto product, List<ValorAtributoProducto> rows) {
        var schema = ProductTechnicalData.schema(product.getCategoria(), categories);
        if (schema == null) return null;
        var current = ProductTechnicalData.read(rows);
        // A saved commercial document, even an explicitly empty one, takes precedence.
        if (current != null) return schema.tipo().equals(current.tipo()) ? current : null;
        var mapping = switch (schema.tipo()) {
            case "cpu" -> Map.ofEntries(Map.entry("socket_cpu","socket"),Map.entry("nucleos_cpu","cores"),
                Map.entry("hilos_cpu","threads"),Map.entry("frecuencia_base_mhz","base_mhz"),
                Map.entry("frecuencia_maxima_mhz","boost_mhz"),Map.entry("cache_l3_kb","l3_mb"),
                Map.entry("potencia_base_w","base_w"),Map.entry("graficos_integrados_cpu","igpu"));
            case "board" -> Map.ofEntries(Map.entry("socket_placa","socket"),Map.entry("chipset_placa","chipset"),
                Map.entry("formato_placa","form_factor"),Map.entry("tipo_ram_placa","memory_type"),
                Map.entry("ranuras_ram_placa","memory_slots"),Map.entry("capacidad_ram_max_placa_gb","memory_max_gb"),
                Map.entry("ranura_pcie_principal","pcie"),Map.entry("ranuras_m2_totales","m2_slots"),
                Map.entry("puertos_sata_totales","sata_ports"),Map.entry("wifi_integrado_placa","wifi"),
                Map.entry("bluetooth_integrado_placa","bluetooth"),Map.entry("velocidad_ethernet_mbps","ethernet_mbps"),
                Map.entry("velocidad_ram_max_placa_mt_s","memory_oc_mts"));
            default -> Map.<String,String>of();
        };
        var data = new LinkedHashMap<String,Object>();
        var notes = new ArrayList<String>();
        for (var row : rows) {
            if (Boolean.TRUE.equals(row.getIsDeleted()) || row.getAtributo()==null || Boolean.TRUE.equals(row.getAtributo().getIsDeleted())) continue;
            String code=row.getAtributo().getCodigo(), field=mapping.get(code);
            if(field==null) continue;
            Object value=row.getValorNumero()!=null?row.getValorNumero():row.getValorBooleano()!=null?row.getValorBooleano():row.getValorTexto();
            if("cache_l3_kb".equals(code)&&value instanceof BigDecimal number) value=number.divide(new BigDecimal("1024"));
            if(value!=null) data.put(field,value);
        }
        UUID sourceId=null;
        if("cpu".equals(schema.tipo())) {
            var cpu=cpus.findById(product.getId());
            if(cpu.isPresent()) {
                var c=cpu.get();
                put(data,notes,"socket",c.getSocket());put(data,notes,"cores",c.getNucleos());put(data,notes,"threads",c.getHilos());
                put(data,notes,"base_mhz",c.getFrecuenciaBaseMhz());put(data,notes,"boost_mhz",c.getFrecuenciaMaximaMhz());
                put(data,notes,"l3_mb",c.getCacheL3Kb()==null?null:c.getCacheL3Kb().divide(new BigDecimal("1024")));
                put(data,notes,"base_w",c.getPotenciaBaseW());put(data,notes,"max_w",c.getPotenciaMaximaW());put(data,notes,"igpu",c.getTieneGraficosIntegrados());
                sourceId=c.getFuenteId();
            }
            if(data.containsKey("base_w")) {
                data.put("power_notes","Valor anterior potencia_base_w. Revisar si corresponde a TDP o PBP; no representa consumo máximo.");
                notes.add("La potencia base anterior no se reclasifica automáticamente como TDP, PPT o MTP.");
            }
        } else if("board".equals(schema.tipo())) {
            var revisions=boards.findByProductoId(product.getId());
            if(revisions.size()==1) {
                var r=revisions.getFirst();put(data,notes,"socket",r.getSocket());put(data,notes,"chipset",r.getChipset());
                put(data,notes,"revision",r.getRevision());sourceId=r.getFuenteId();
            } else if(revisions.size()>1) notes.add("Hay varias revisiones de placa. No se seleccionó una automáticamente; revisa la versión exacta.");
        }
        if(data.isEmpty()) return null;
        notes.add(0,"Datos recuperados de la ficha anterior. Guardar aquí crea una ficha comercial pendiente de revisión y no cambia la ficha del configurador.");
        String source=null;
        if(sourceId!=null) {
            var urls=jdbc.query("select url from rya.fuentes_tecnicas where id = ?",(rs,n)->rs.getString(1),sourceId);
            if(!urls.isEmpty()) source=urls.getFirst();
        }
        return new FichaProductoDto(schema.tipo(),source,data,"ANTERIOR",List.copyOf(notes));
    }
    private void put(Map<String,Object> data,List<String> notes,String field,Object value) {
        if(value==null) return;
        var old=data.get(field);
        boolean same=old instanceof Number a && value instanceof Number b
            ?new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()))==0:Objects.equals(old,value);
        if(old!=null&&!same) notes.add("Diferencia en "+field+": se muestra la ficha normalizada; el atributo anterior se conserva sin modificar.");
        data.put(field,value);
    }
}
