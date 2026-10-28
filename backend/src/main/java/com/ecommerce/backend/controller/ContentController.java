package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/contenido")
public class ContentController {
    private final AdminContentService service;

    @GetMapping
    public List<Map<String, Object>> published(
            @RequestParam @Pattern(regexp = "inicio|experiencias") String ubicacion) {
        return service.published(ubicacion);
    }
}
