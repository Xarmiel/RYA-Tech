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

    @GetMapping("/preguntas")
    public AyudaDto preguntas(@RequestParam(required=false) @Size(max=200) String q,
            @RequestParam(required=false) UUID categoriaId,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(100) int size) {
        return service.preguntas(q,categoriaId,page,size);
    }

    @GetMapping("/contacto")
    public Contacto contacto() {
        String phone = limpio(soporte.getTelefono());
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        String whatsapp = limpio(soporte.getWhatsapp());
        return new Contacto(limpio(soporte.getEmail()),phone,
                digits.isEmpty() || digits.matches("0+") ? null : "tel:" + phone.replaceAll("[^+0-9]", ""),
                whatsapp == null ? null : "https://wa.me/" + whatsapp,limpio(soporte.getHorario()));
    }

    public record Contacto(String email,String telefono,String telefonoUrl,String whatsappUrl,String horario) {}
    private String limpio(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
