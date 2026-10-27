package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.ProductImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/productos/{id}/importacion")
@RequiredArgsConstructor
public class ProductImportController {
    private final ProductImportService service;
    @GetMapping public ProductImportService.Snapshot preview(@PathVariable UUID id) { return service.preview(id); }
    @PatchMapping public ProductImportService.Snapshot resolve(@PathVariable UUID id,@Valid @RequestBody ProductImportService.Resolution request) {
        return service.resolve(id,request);
    }
}
