package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.FamiliaPerfilAsesoriaDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.PerfilAsesoriaDto;

import java.util.List;
import java.util.UUID;

public interface AdminAsesoriaService {
    PaginatedResponseDto<PerfilAsesoriaDto> listarPerfiles(String busqueda, Boolean activo, UUID familiaId, int page, int size);
    PerfilAsesoriaDto obtenerPerfilPorId(UUID id);
    PerfilAsesoriaDto crearPerfil(PerfilAsesoriaDto dto);
    PerfilAsesoriaDto actualizarPerfil(UUID id, PerfilAsesoriaDto dto);
    PerfilAsesoriaDto cambiarEstado(UUID id, boolean activo);
    void eliminarPerfil(UUID id);
    List<FamiliaPerfilAsesoriaDto> listarFamilias();
    FamiliaPerfilAsesoriaDto crearFamilia(FamiliaPerfilAsesoriaDto dto);
}
