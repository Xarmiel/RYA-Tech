package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.SoporteProperties;
import com.ecommerce.backend.dto.AyudaDto;
import com.ecommerce.backend.service.AyudaService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ayuda")
@RequiredArgsConstructor
@Validated
public class AyudaController {
    private final AyudaService service;
    private final SoporteProperties soporte;
    private final com.ecommerce.backend.service.AdminContentService contenido;

    @GetMapping("/preguntas")
    public AyudaDto preguntas(@RequestParam(required=false) @Size(max=200) String q,
            @RequestParam(required=false) UUID categoriaId,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(100) int size) {
        return service.preguntas(q,categoriaId,page,size);
    }

    @GetMapping("/contacto")
    public Contacto contacto() {
        var saved=contenido.contact();
        String phone = limpio(saved.get("telefono")==null ? soporte.getTelefono() : saved.get("telefono").toString());
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        String whatsapp = limpio(saved.get("whatsapp")==null ? soporte.getWhatsapp() : saved.get("whatsapp").toString());
        return new Contacto(limpio(saved.get("email")==null ? soporte.getEmail() : saved.get("email").toString()),phone,
                digits.isEmpty() || digits.matches("0+") ? null : "tel:" + phone.replaceAll("[^+0-9]", ""),
                whatsapp == null ? null : "https://wa.me/" + whatsapp,limpio(saved.get("horario")==null ? soporte.getHorario() : saved.get("horario").toString()));
    }

    public record Contacto(String email,String telefono,String telefonoUrl,String whatsappUrl,String horario) {}
    private String limpio(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
