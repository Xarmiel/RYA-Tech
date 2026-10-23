package com.ecommerce.backend.technicaldraft;

import com.ecommerce.backend.config.AdminPermissions;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TechnicalDraftService {
    private final TechnicalDraftRepository drafts;
    private final TechnicalDraftEventRepository events;
    private final TechnicalDraftValidator validator;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final TechnicalDraftFicha fichas;
    private static final Set<String> STATES=Set.of("DRAFT","IN_REVIEW","APPROVED","REJECTED");
    public TechnicalDraftService(TechnicalDraftRepository drafts,TechnicalDraftEventRepository events,
                                 TechnicalDraftValidator validator,ObjectMapper mapper,TechnicalDraftFicha fichas,
                                 @Value("${app.technical-drafts.enabled:${APP_TECHNICAL_DRAFTS_ENABLED:false}}") boolean enabled){
        this.drafts=drafts;this.events=events;this.validator=validator;this.mapper=mapper;this.fichas=fichas;this.enabled=enabled;
    }
    private UUID actor(Jwt jwt){
        if(!AdminPermissions.allowed(jwt))throw new DraftFault(403,"Se requiere un administrador autorizado.");
        if(!enabled)throw new DraftFault(503,"Borradores técnicos desactivados: aplicar primero la migración aprobada y habilitar app.technical-drafts.enabled.");
        try{return UUID.fromString(jwt.getSubject());}catch(Exception ex){throw new DraftFault(403,"Identidad administrativa inválida.");}
    }
    private TechnicalDraft load(UUID id){return drafts.findById(id).orElseThrow(()->new DraftFault(404,"Borrador técnico no encontrado."));}
    private void version(TechnicalDraft draft,Long expected){if(expected==null||!expected.equals(draft.getVersion()))throw new DraftFault(409,"El borrador cambió. Recarga su versión antes de continuar.");}
    private void owner(TechnicalDraft draft,UUID actor){if(!draft.getCreatedBy().equals(actor))throw new DraftFault(403,"Solo su autor puede editar, enviar o reabrir este borrador.");}
    private JsonNode parsed(TechnicalDraft draft){try{return mapper.readTree(draft.getDocumentJson());}catch(Exception ex){throw new DraftFault(500,"No se pudo recuperar el documento técnico.");}}
    private JsonNode valid(JsonNode document,boolean submission){var result=validator.validate(document,submission);if(!result.valid())throw new DraftFault(400,"Revisa los datos del borrador técnico.",result.issues());fichas.validateLink(result.document());return result.document();}
    private String serialize(JsonNode document){try{return mapper.writeValueAsString(document);}catch(Exception ex){throw new DraftFault(400,"Documento inválido.");}}
    private TechnicalDraftDtos.Detail detail(TechnicalDraft draft){return new TechnicalDraftDtos.Detail(draft.getId(),draft.getVersion(),draft.getEstado(),"pending",draft.getCreatedBy(),draft.getUpdatedBy(),draft.getCreatedAt(),draft.getUpdatedAt(),parsed(draft));}
    private void document(TechnicalDraft draft,JsonNode document){
        draft.setDocumentJson(serialize(document));draft.setNombre(document.path("product").path("name").asText().trim());
        draft.setSku(document.path("product").path("sku").asText().trim());draft.setTipo(document.path("product").path("type").asText());
    }
    private TechnicalDraftDtos.Detail save(TechnicalDraft draft,UUID actor,String action,String comment){
        draft.setUpdatedBy(actor);draft.setUpdatedAt(OffsetDateTime.now());drafts.saveAndFlush(draft);
        TechnicalDraftEvent event=new TechnicalDraftEvent();event.setBorradorId(draft.getId());event.setVersion(draft.getVersion());
        event.setActor(actor);event.setAccion(action);event.setEstado(draft.getEstado());event.setComentario(comment==null?"":comment.trim());event.setCreatedAt(draft.getUpdatedAt());
        event.setDocumentJson(draft.getDocumentJson());
        events.save(event);return detail(draft);
    }
    @Transactional
    public TechnicalDraftDtos.Detail create(Jwt jwt,JsonNode document){
        UUID actor=actor(jwt);TechnicalDraft draft=new TechnicalDraft();document(draft,valid(document,false));
        draft.setEstado("DRAFT");draft.setCreatedBy(actor);draft.setCreatedAt(OffsetDateTime.now());return save(draft,actor,"CREATED","");
    }
    @Transactional(readOnly=true)
    public TechnicalDraftDtos.Detail get(Jwt jwt,UUID id){actor(jwt);return detail(load(id));}
    @Transactional(readOnly=true)
    public TechnicalDraftFicha.View ficha(Jwt jwt,UUID id){actor(jwt);return fichas.preview(parsed(load(id)));}
    @Transactional
    public TechnicalDraftDtos.Detail link(Jwt jwt,UUID id,Long expected,boolean unlink){
        UUID actor=actor(jwt);var draft=load(id);owner(draft,actor);version(draft,expected);
        if(!draft.getEstado().equals("DRAFT"))throw new DraftFault(409,"Reabre el borrador antes de cambiar su vínculo.");
        var doc=(com.fasterxml.jackson.databind.node.ObjectNode)parsed(draft);
        if(unlink)doc.remove("catalogProductId");else doc.put("catalogProductId",fichas.resolve(doc).getId().toString());
        document(draft,valid(doc,false));return save(draft,actor,"UPDATED",unlink?"Vínculo comercial retirado.":"Vinculado a producto real: "+doc.path("catalogProductId").asText());
    }
    @Transactional(readOnly=true)
    public PaginatedResponseDto<TechnicalDraftDtos.Summary> list(Jwt jwt,int page,int size,String q,String state){
        actor(jwt);if(page<0||size<1||size>50)throw new DraftFault(400,"Paginación inválida: tamaño entre 1 y 50.");
        if(q!=null&&q.length()>220)throw new DraftFault(400,"La búsqueda admite hasta 220 caracteres.");
        if(state!=null&&!STATES.contains(state))throw new DraftFault(400,"Estado de borrador inválido.");
        Specification<TechnicalDraft> filter=(root,query,cb)->{
            var predicate=cb.conjunction();
            if(state!=null)predicate=cb.and(predicate,cb.equal(root.get("estado"),state));
            if(q!=null&&!q.isBlank()){
                String escaped=q.trim().toLowerCase(java.util.Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                predicate=cb.and(predicate,cb.or(cb.like(cb.lower(root.get("nombre")),"%"+escaped+"%",'\\'),cb.like(cb.lower(root.get("sku")),"%"+escaped+"%",'\\')));
            }return predicate;
        };
        var result=drafts.findAll(filter,PageRequest.of(page,size,Sort.by(Sort.Order.desc("updatedAt"),Sort.Order.desc("id"))));
        List<TechnicalDraftDtos.Summary> items=result.map(d->new TechnicalDraftDtos.Summary(d.getId(),d.getVersion(),d.getNombre(),d.getSku(),d.getTipo(),d.getEstado(),d.getCreatedBy(),d.getUpdatedAt())).getContent();
        return new PaginatedResponseDto<>(items,page,size,result.getTotalElements(),result.getTotalPages(),result.isLast(),result.isFirst());
    }
    @Transactional
    public TechnicalDraftDtos.Detail update(Jwt jwt,UUID id,TechnicalDraftDtos.Update request){
        UUID actor=actor(jwt);TechnicalDraft draft=load(id);owner(draft,actor);version(draft,request.version());
        if(!draft.getEstado().equals("DRAFT"))throw new DraftFault(409,"Reabre el borrador antes de editarlo; una revisión no se modifica en curso.");
        document(draft,valid(request.document(),false));return save(draft,actor,"UPDATED","");
    }
    @Transactional
    public TechnicalDraftDtos.Detail submit(Jwt jwt,UUID id,Long expected){
        UUID actor=actor(jwt);TechnicalDraft draft=load(id);owner(draft,actor);version(draft,expected);
        if(!draft.getEstado().equals("DRAFT"))throw new DraftFault(409,"Solo un borrador editable puede enviarse a revisión.");
        document(draft,valid(parsed(draft),true));draft.setEstado("IN_REVIEW");return save(draft,actor,"SUBMITTED","");
    }
    @Transactional
    public TechnicalDraftDtos.Detail decide(Jwt jwt,UUID id,TechnicalDraftDtos.Decision request){
        UUID actor=actor(jwt);TechnicalDraft draft=load(id);version(draft,request.version());
        if(draft.getCreatedBy().equals(actor))throw new DraftFault(403,"La revisión requiere un administrador distinto del autor.");
        if(!draft.getEstado().equals("IN_REVIEW"))throw new DraftFault(409,"El borrador no está en revisión.");
        if(!Set.of("APPROVED","REJECTED").contains(request.decision()))throw new DraftFault(400,"Decisión de revisión inválida.");
        if(request.comment()==null||request.comment().isBlank()||request.comment().length()>2000)throw new DraftFault(400,"Indica el motivo de la revisión (hasta 2000 caracteres).");
        if(request.decision().equals("APPROVED"))valid(parsed(draft),true);
        draft.setEstado(request.decision());return save(draft,actor,"REVIEWED",request.comment());
    }
    @Transactional
    public TechnicalDraftDtos.Detail reopen(Jwt jwt,UUID id,TechnicalDraftDtos.Transition request){
        UUID actor=actor(jwt);TechnicalDraft draft=load(id);owner(draft,actor);version(draft,request.version());
        if(!Set.of("APPROVED","REJECTED").contains(draft.getEstado()))throw new DraftFault(409,"Solo un borrador revisado puede reabrirse.");
        draft.setEstado("DRAFT");return save(draft,actor,"REOPENED",request.comment());
    }
    @Transactional(readOnly=true)
    public PaginatedResponseDto<TechnicalDraftDtos.Event> history(Jwt jwt,UUID id,int page,int size){
        actor(jwt);load(id);if(page<0||size<1||size>50)throw new DraftFault(400,"Paginación inválida.");
        var result=events.findByBorradorId(id,PageRequest.of(page,size,Sort.by("version")));
        var items=result.map(e->new TechnicalDraftDtos.Event(e.getId(),e.getVersion(),e.getActor(),e.getAccion(),e.getEstado(),e.getComentario(),e.getCreatedAt())).getContent();
        return new PaginatedResponseDto<>(items,page,size,result.getTotalElements(),result.getTotalPages(),result.isLast(),result.isFirst());
    }
    public TechnicalDraftDtos.Validation validate(Jwt jwt,JsonNode document,boolean submission){actor(jwt);return validator.validate(document,submission);}
    @Transactional(readOnly=true)
    public TechnicalDraftDtos.Revision revision(Jwt jwt,UUID id,Long version){
        actor(jwt);load(id);var event=events.findByBorradorIdAndVersion(id,version).orElseThrow(()->new DraftFault(404,"Versión no encontrada."));
        try{return new TechnicalDraftDtos.Revision(new TechnicalDraftDtos.Event(event.getId(),event.getVersion(),event.getActor(),event.getAccion(),event.getEstado(),event.getComentario(),event.getCreatedAt()),"pending",mapper.readTree(event.getDocumentJson()));}
        catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new DraftFault(500,"No se pudo recuperar la versión histórica.");}
    }
}
