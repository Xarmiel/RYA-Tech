package com.ecommerce.backend.service;
import com.ecommerce.backend.dto.TarifaEnvioRequest;
import com.ecommerce.backend.entity.TarifaEnvio;
import com.ecommerce.backend.repository.TarifaEnvioRepository;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class EnvioAdminService {
    private final TarifaEnvioRepository tarifas;
    private final JdbcTemplate jdbc;
    public List<TarifaEnvio> listar() { return tarifas.findByIsDeletedFalseOrderByDepartamentoAsc(); }
    @Transactional
    public TarifaEnvio guardar(UUID id, TarifaEnvioRequest data) {
        TarifaEnvio tarifa;
        if (id == null) { tarifa=new TarifaEnvio(); tarifa.setId(UUID.randomUUID()); tarifa.setIsDeleted(false); }
        else tarifa=tarifas.findById(id).filter(t -> !Boolean.TRUE.equals(t.getIsDeleted())).orElseThrow(() -> new ResourceNotFoundException("Tarifa no encontrada"));
        if ("RECOJO".equals(data.modalidad())) {
            if (blank(data.direccionRecojo()) || blank(data.horarioRecojo())) throw new BadRequestException("El recojo requiere dirección y horario.");
            if (data.costo().signum() != 0) throw new BadRequestException("El recojo debe tener costo cero.");
        } else if (blank(data.departamento())) throw new BadRequestException("Indica al menos un departamento de cobertura.");
        if (!blank(data.distrito()) && blank(data.provincia())) throw new BadRequestException("Un distrito requiere provincia.");
        tarifa.setNombreServicio(data.nombreServicio().trim()); tarifa.setModalidad(data.modalidad());
        tarifa.setTransportistaId(null); tarifa.setDepartamento(clean(data.departamento())); tarifa.setProvincia(clean(data.provincia())); tarifa.setDistrito(clean(data.distrito()));
        tarifa.setCosto(data.costo()); tarifa.setPlazoDias(data.plazoDias()); tarifa.setActivo(data.activo());
        tarifa.setDireccionRecojo("RECOJO".equals(data.modalidad()) ? clean(data.direccionRecojo()) : null);
        tarifa.setHorarioRecojo("RECOJO".equals(data.modalidad()) ? clean(data.horarioRecojo()) : null);
        return tarifas.save(tarifa);
    }
    private static boolean blank(String v) { return v == null || v.isBlank(); }
    private static String clean(String v) { return blank(v) ? null : v.trim(); }
    public Map<String,Object> pedidos(int page) {
        var rows=jdbc.query("SELECT id,codigo,estado,nombre_completo,modalidad_entrega,direccion_envio,referencia_envio,entrega_snapshot,estado_entrega,transportista_seguimiento,codigo_seguimiento,updated_at FROM rya.pedidos WHERE is_deleted=false AND estado='PAGADO' ORDER BY created_at DESC,id LIMIT 30 OFFSET ?", (rs,index)->{
            Map<String,Object> row=new LinkedHashMap<>();
            for(String field:List.of("id","codigo","estado","nombre_completo","modalidad_entrega","direccion_envio","referencia_envio","entrega_snapshot","estado_entrega","transportista_seguimiento","codigo_seguimiento")) row.put(field,rs.getString(field));
            row.put("updated_at",rs.getObject("updated_at",java.time.OffsetDateTime.class));return row;
        }, (long)page*30);
        Long total=jdbc.queryForObject("SELECT count(*) FROM rya.pedidos WHERE is_deleted=false AND estado='PAGADO'",Long.class);
        return Map.of("items",rows,"total",total,"page",page);
    }
    @Transactional
    public void seguimiento(UUID id, String estado, String transportista, String codigo, java.time.OffsetDateTime version) {
        var rows=jdbc.queryForList("SELECT estado,modalidad_entrega,estado_entrega,updated_at FROM rya.pedidos WHERE id=? AND is_deleted=false FOR UPDATE",id);
        if(rows.isEmpty()) throw new ResourceNotFoundException("Pedido no encontrado");
        var row=rows.getFirst();
        if(!"PAGADO".equals(row.get("estado"))) throw new BadRequestException("Solo se despachan pedidos con pago confirmado.");
        boolean recojo="RECOJO".equals(row.get("modalidad_entrega"));
        String current=Objects.toString(row.get("estado_entrega"),"PENDIENTE");
        var allowed=recojo ? List.of("PENDIENTE","PREPARANDO","LISTO_RECOJO","RETIRADO") : List.of("PENDIENTE","PREPARANDO","ENVIADO","ENTREGADO");
        if(!allowed.contains(estado) || allowed.indexOf(estado)<allowed.indexOf(current) || allowed.indexOf(estado)>allowed.indexOf(current)+1)
            throw new BadRequestException("Actualiza la entrega en orden, sin retroceder ni saltar etapas.");
        if(!recojo && List.of("ENVIADO","ENTREGADO").contains(estado) && (blank(transportista) || blank(codigo)))
            throw new BadRequestException("Para despachar indica transportista y código de seguimiento.");
        int updated=jdbc.update("UPDATE rya.pedidos SET estado_entrega=?,transportista_seguimiento=?,codigo_seguimiento=?,updated_at=GREATEST(CURRENT_TIMESTAMP, updated_at + INTERVAL '0.001' SECOND) WHERE id=? AND updated_at=?",estado,recojo?null:clean(transportista),recojo?null:clean(codigo),id,version);
        if(updated!=1) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"El pedido cambió. Actualiza el listado antes de guardar.");
    }
}
