package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.EspecialidadPerfilCreateDto;
import com.ecommerce.backend.dto.response.EspecialidadPerfilResponseDto;
import com.ecommerce.backend.service.AsesoriaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asesorias")
@RequiredArgsConstructor
public class AsesoriaAdminController {

    private final AsesoriaAdminService asesoriaAdminService;

    @PostMapping
    public ResponseEntity<EspecialidadPerfilResponseDto> crearEspecialidadConPerfil(
            @Valid @RequestBody EspecialidadPerfilCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asesoriaAdminService.crearEspecialidadConPerfil(dto));
    }

    @GetMapping
    public ResponseEntity<List<EspecialidadPerfilResponseDto>> listarTodas() {
        return ResponseEntity.ok(asesoriaAdminService.listarTodas());
    }
}
