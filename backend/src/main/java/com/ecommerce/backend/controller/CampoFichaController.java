package com.ecommerce.backend.controller;
import com.ecommerce.backend.service.CampoFichaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/categorias/{categoriaId}/campos-ficha")
public class CampoFichaController {
 private final CampoFichaService service;
 @GetMapping public CampoFichaService.Result list(@PathVariable UUID categoriaId){return service.list(categoriaId);}
 @PostMapping public CampoFichaService.Result create(@PathVariable UUID categoriaId,@Valid @RequestBody CampoFichaService.Request data,@AuthenticationPrincipal Jwt jwt){return service.save(categoriaId,null,data,UUID.fromString(jwt.getSubject()));}
 @PutMapping("/{id}") public CampoFichaService.Result edit(@PathVariable UUID categoriaId,@PathVariable UUID id,@Valid @RequestBody CampoFichaService.Request data,@AuthenticationPrincipal Jwt jwt){return service.save(categoriaId,id,data,UUID.fromString(jwt.getSubject()));}
}
