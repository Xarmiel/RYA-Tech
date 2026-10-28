package com.ecommerce.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.exception.*;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.entity.*;
import java.util.*;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTechnicalService {
    private final AdminOperationsService ops;
    private final ProductoService products;
    private final CompatibilidadCpuPlacaRepository relations;
    private final EspecificacionCpuRepository cpus;
    private final RevisionPlacaRepository boards;
    private final VersionBiosRepository bios;

    public Map<String, Object> references() {
        return Map.of("cpus", ops.rows(
                "SELECT c.producto_id AS id,p.nombre,p.sku,c.socket FROM rya.especificaciones_cpu c JOIN rya.productos p ON p.id=c.producto_id WHERE p.is_deleted=false ORDER BY p.nombre"),
                "placas",
                ops.rows(
                        "SELECT r.id,p.nombre,p.sku,r.revision,r.socket FROM rya.revisiones_placa r JOIN rya.productos p ON p.id=r.producto_id WHERE p.is_deleted=false ORDER BY p.nombre,r.revision"),
                "bios", ops.rows("SELECT id,placa_revision_id,version FROM rya.versiones_bios ORDER BY orden"),
                "fuentes", ops.rows("SELECT id,titulo,url FROM rya.fuentes_tecnicas ORDER BY titulo"));
    }

    public Map<String, Object> list(int page) {
        return ops.page(
                "SELECT c.cpu_id,c.placa_revision_id,p.nombre AS cpu,p.sku AS cpu_sku,b.nombre AS placa,r.revision,c.soporte,c.condiciones,c.bios_minima_id,c.fuente_id,f.url,c.verificada_at,c.verificada_at AS created_at",
                "FROM rya.compatibilidad_cpu_placa c JOIN rya.productos p ON p.id=c.cpu_id JOIN rya.revisiones_placa r ON r.id=c.placa_revision_id JOIN rya.productos b ON b.id=r.producto_id JOIN rya.fuentes_tecnicas f ON f.id=c.fuente_id ",
                "WHERE p.is_deleted=false AND b.is_deleted=false", page);
    }

    @Transactional
    public void save(UUID cpuId, UUID boardId, UUID biosId, UUID sourceId, String support, String conditions,
            OffsetDateTime previous, boolean evidence, UUID actor) {
        ops.require("SELECT producto_id FROM rya.especificaciones_cpu WHERE producto_id=? FOR UPDATE", cpuId);
        var cpu = cpus.findById(cpuId)
                .orElseThrow(() -> new ResourceNotFoundException("Falta la ficha normalizada del procesador"));
        var board = boards.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Revisión de placa no encontrada"));
        var source = ops.require("SELECT url FROM rya.fuentes_tecnicas WHERE id=?", sourceId).getFirst();
        String url = Objects.toString(source.get("url"), "");
        AdminOperationsService.safeUrl(url);
        if (url.isBlank() || !evidence)
            throw new BadRequestException("Revisa la fuente oficial y confirma la evidencia antes de guardar.");
        if ("SOPORTADO".equals(support) && !cpu.getSocket().equalsIgnoreCase(board.getSocket()))
            throw new BadRequestException("No se puede certificar soporte con sockets diferentes.");
        if (biosId != null && !bios.findById(biosId).filter(v -> boardId.equals(v.getPlacaRevisionId())).isPresent())
            throw new BadRequestException("La BIOS debe pertenecer a la revisión de placa seleccionada.");
        var current = ops.rows(
                "SELECT verificada_at FROM rya.compatibilidad_cpu_placa WHERE cpu_id=? AND placa_revision_id=? FOR UPDATE",
                cpuId, boardId);
        if (!current.isEmpty() && (previous == null || !((OffsetDateTime) current.getFirst().get("verificada_at"))
                .toInstant().equals(previous.toInstant())))
            throw new ConflictException("Esta relación cambió. Actualiza y vuelve a revisarla.");
        if (current.isEmpty() && previous != null)
            throw new ConflictException("La relación ya no existe.");
        var model = CompatibilidadCpuPlaca.builder().id(new CompatibilidadCpuPlacaId(cpuId, boardId)).soporte(support)
                .biosMinimaId(biosId).fuenteId(sourceId).condiciones(conditions).verificadaAt(OffsetDateTime.now())
                .build();
        relations.saveAndFlush(model);
        ops.audit(actor, "compatibilidad", cpuId + ":" + boardId, support, conditions);
    }

    public Map<String, Object> quality(String search, int page) {
        var result = products.listarProductosAdmin(search, null, null, null, page, 20, "nombre", "asc");
        var rows = result.getContent().stream().map(product -> {
            var detail = products.obtenerPorIdAdmin(product.getId());
            List<String> issues = new ArrayList<>();
            if (detail.getImagenes() == null || detail.getImagenes().isEmpty())
                issues.add("Sin imágenes");
            var ficha = detail.getFichaTecnica();
            if (ficha == null)
                issues.add("Sin ficha técnica");
            else {
                if (ficha.fuenteUrl() == null || ficha.fuenteUrl().isBlank())
                    issues.add("Sin fuente de especificaciones");
                if (ficha.valores() == null || ficha.valores().isEmpty())
                    issues.add("Sin especificaciones");
                if (ficha.avisos() != null)
                    issues.addAll(ficha.avisos());
            }
            if (detail.getMarca() == null)
                issues.add("Sin marca");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", product.getId());
            row.put("sku", product.getSku());
            row.put("nombre", product.getNombre());
            row.put("incidencias", issues);
            row.put("prioridad", issues.isEmpty() ? "REVISAR EVIDENCIA" : "COMPLETAR DATOS");
            row.put("compatibilidad", "La ficha comercial no certifica compatibilidad");
            return row;
        }).toList();
        return Map.of("items", rows, "total", result.getTotalElements(), "page", page);
    }
}
