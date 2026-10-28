package com.ecommerce.backend.controller;
import com.ecommerce.backend.service.AdminTechnicalService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
import java.time.OffsetDateTime;
@RestController @RequiredArgsConstructor @Validated @RequestMapping("/api/v1/admin/tecnica")
public class AdminTechnicalController {
 private final AdminTechnicalService service;
 public record Relation(@NotNull UUID cpuId,@NotNull UUID placaRevisionId,UUID biosId,@NotNull UUID fuenteId,@NotBlank @Pattern(regexp="SOPORTADO|NO_SOPORTADO|POR_VERIFICAR") String soporte,@NotBlank @Size(min=10,max=3000) String condiciones,OffsetDateTime version,@AssertTrue boolean evidenciaRevisada){}
 @GetMapping("/referencias") public Map<String,Object> references(){return service.references();}
 @GetMapping("/compatibilidad") public Map<String,Object> list(@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page){return service.list(page);}
 @PostMapping("/compatibilidad") public void save(@Valid @RequestBody Relation data,@AuthenticationPrincipal Jwt jwt){service.save(data.cpuId(),data.placaRevisionId(),data.biosId(),data.fuenteId(),data.soporte(),data.condiciones(),data.version(),data.evidenciaRevisada(),UUID.fromString(jwt.getSubject()));}
 @GetMapping("/calidad") public Map<String,Object> quality(@RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page){return service.quality(q,page);}
}
