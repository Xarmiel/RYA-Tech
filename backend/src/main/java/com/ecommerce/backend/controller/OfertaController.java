package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.OfertasResponseDto;
import com.ecommerce.backend.service.OfertaService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/ofertas")
@RequiredArgsConstructor
@Validated
public class OfertaController {
    private final OfertaService service;

    @GetMapping
    public OfertasResponseDto listar(@RequestParam(required=false) @Size(max=200) String q,
            @RequestParam(required=false) String categoria, @RequestParam(required=false) String marca,
            @RequestParam(defaultValue="false") boolean enStock,
            @RequestParam(required=false) @DecimalMin("0") @DecimalMax("100") BigDecimal descuentoMin,
            @RequestParam(defaultValue="descuento") String orden,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(100) int size) {
        return service.listar(q, categoria, marca, enStock, descuentoMin, orden, page, size);
    }
}
