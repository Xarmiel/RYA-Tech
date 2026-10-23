package com.ecommerce.backend.technicaldraft;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/borradores-tecnicos")
@RequiredArgsConstructor
public class TechnicalDraftController {
    private final TechnicalDraftService service;
    @PostMapping
    public ResponseEntity<TechnicalDraftDtos.Detail> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TechnicalDraftDtos.Create request){
        var result=service.create(jwt,request.document());return ResponseEntity.created(URI.create("/api/v1/admin/borradores-tecnicos/"+result.id())).body(result);
    }
    @GetMapping
    public PaginatedResponseDto<TechnicalDraftDtos.Summary> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String q,@RequestParam(required=false) String state){return service.list(jwt,page,size,q,state);}
    @GetMapping("/{id}") public TechnicalDraftDtos.Detail get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.get(jwt,id);}
    @GetMapping("/{id}/ficha") public TechnicalDraftFicha.View ficha(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.ficha(jwt,id);}
    @PostMapping("/{id}/vinculo") public TechnicalDraftDtos.Detail link(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody TechnicalDraftDtos.Transition request,@RequestParam(defaultValue="false") boolean unlink){return service.link(jwt,id,request.version(),unlink);}
    @PutMapping("/{id}") public TechnicalDraftDtos.Detail update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody TechnicalDraftDtos.Update request){return service.update(jwt,id,request);}
    @PostMapping("/{id}/revision") public TechnicalDraftDtos.Detail submit(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody TechnicalDraftDtos.Transition request){return service.submit(jwt,id,request.version());}
    @PostMapping("/{id}/decision") public TechnicalDraftDtos.Detail decide(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody TechnicalDraftDtos.Decision request){return service.decide(jwt,id,request);}
    @PostMapping("/{id}/reabrir") public TechnicalDraftDtos.Detail reopen(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody TechnicalDraftDtos.Transition request){return service.reopen(jwt,id,request);}
    @GetMapping("/{id}/historial") public PaginatedResponseDto<TechnicalDraftDtos.Event> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return service.history(jwt,id,page,size);}
    @GetMapping("/{id}/versiones/{version}") public TechnicalDraftDtos.Revision revision(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable Long version){return service.revision(jwt,id,version);}
    @PostMapping("/validar") public TechnicalDraftDtos.Validation validate(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TechnicalDraftDtos.Create request,@RequestParam(defaultValue="false") boolean submission){return service.validate(jwt,request.document(),submission);}
}
