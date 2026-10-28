package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/contenido")
public class AdminContentController {
    private final AdminContentService service;

    public record Content(UUID id, @NotBlank @Pattern(regexp = "FAQ|BANNER|EXPERIENCIA") String tipo,
            @NotBlank @Size(max = 200) String titulo, @NotBlank @Size(max = 10000) String texto,
            @NotBlank @Pattern(regexp = "inicio|experiencias") String ubicacion, UUID categoriaId,
            @Min(0) @Max(10000) int posicion, boolean publicada, @Size(max = 2000) String enlace,
            @Size(max = 2000) String imagenUrl, @Min(0) long version) {
    }

    public record Contact(@Email @Size(max = 254) String email,
            @Pattern(regexp = "^$|\\+?[0-9 ()-]{7,24}") String telefono,
            @Pattern(regexp = "^$|[0-9]{8,15}") String whatsapp, @Size(max = 200) String horario,
            @Min(0) long version) {
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "FAQ") @Pattern(regexp = "FAQ|BANNER|EXPERIENCIA") String tipo,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return service.list(tipo, page);
    }

    @GetMapping("/categorias")
    public List<Map<String, Object>> categories() {
        return service.categories();
    }

    @PostMapping
    public Map<String, UUID> save(@Valid @RequestBody Content data, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("id",
                service.save(data.id(), data.tipo(), data.titulo(), data.texto(), data.ubicacion(), data.categoriaId(),
                        data.posicion(), data.publicada(), data.enlace(), data.imagenUrl(), data.version(),
                        UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/contacto")
    public Map<String, Object> contact() {
        return service.contact();
    }

    @PutMapping("/contacto")
    public void contact(@Valid @RequestBody Contact data, @AuthenticationPrincipal Jwt jwt) {
        service.contact(data.email(), data.telefono(), data.whatsapp(), data.horario(), data.version(),
                UUID.fromString(jwt.getSubject()));
    }
}
