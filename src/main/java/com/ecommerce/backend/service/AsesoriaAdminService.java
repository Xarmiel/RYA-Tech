package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.EspecialidadPerfilCreateDto;
import com.ecommerce.backend.dto.response.EspecialidadPerfilResponseDto;
import java.util.List;

public interface AsesoriaAdminService {
    EspecialidadPerfilResponseDto crearEspecialidadConPerfil(EspecialidadPerfilCreateDto dto);
    List<EspecialidadPerfilResponseDto> listarTodas();
}
