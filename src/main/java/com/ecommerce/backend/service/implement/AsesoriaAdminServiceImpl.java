package com.ecommerce.backend.service.implement;

import com.ecommerce.backend.dto.request.EspecialidadPerfilCreateDto;
import com.ecommerce.backend.dto.response.EspecialidadPerfilResponseDto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.model.Especialidad;
import com.ecommerce.backend.model.PerfilHardware;
import com.ecommerce.backend.repository.EspecialidadRepository;
import com.ecommerce.backend.repository.PerfilHardwareRepository;
import com.ecommerce.backend.service.AsesoriaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AsesoriaAdminServiceImpl implements AsesoriaAdminService {

    private final EspecialidadRepository especialidadRepository;
    private final PerfilHardwareRepository perfilHardwareRepository;

    @Override
    @Transactional
    public EspecialidadPerfilResponseDto crearEspecialidadConPerfil(EspecialidadPerfilCreateDto dto) {
        if (especialidadRepository.findByNombreIgnoreCase(dto.nombre().trim()).isPresent()) {
            throw new BadRequestException("Ya existe una especialidad con el nombre: " + dto.nombre().trim());
        }

        Short[] prioridades = {
            dto.prioCpu(), dto.prioGpu(), dto.prioDisco(),
            dto.prioRam(), dto.prioFuente(), dto.prioPlaca(), dto.prioCase()
        };

        Set<Short> prioSet = new HashSet<>(Arrays.asList(prioridades));
        if (prioSet.size() != 7) {
            throw new BadRequestException("Los niveles de prioridad para cada componente deben ser del 1 al 7 sin repetirse.");
        }

        for (Short p : prioridades) {
            if (p == null || p < 1 || p > 7) {
                throw new BadRequestException("Las prioridades deben ser valores enteros entre 1 y 7.");
            }
        }

        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(dto.nombre().trim());
        especialidad.setCampo(dto.campo().trim());
        especialidad = especialidadRepository.save(especialidad);

        PerfilHardware perfil = new PerfilHardware();
        perfil.setEspecialidad(especialidad);
        perfil.setPrioCpu(dto.prioCpu());
        perfil.setPrioGpu(dto.prioGpu());
        perfil.setPrioDisco(dto.prioDisco());
        perfil.setPrioRam(dto.prioRam());
        perfil.setPrioFuente(dto.prioFuente());
        perfil.setPrioPlaca(dto.prioPlaca());
        perfil.setPrioCase(dto.prioCase());
        perfil.setDescripcion(dto.descripcion().trim());

        perfil = perfilHardwareRepository.save(perfil);

        return new EspecialidadPerfilResponseDto(
            especialidad.getId(),
            especialidad.getNombre(),
            especialidad.getCampo(),
            perfil.getId(),
            perfil.getPrioCpu(),
            perfil.getPrioGpu(),
            perfil.getPrioDisco(),
            perfil.getPrioRam(),
            perfil.getPrioFuente(),
            perfil.getPrioPlaca(),
            perfil.getPrioCase(),
            perfil.getDescripcion()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspecialidadPerfilResponseDto> listarTodas() {
        return perfilHardwareRepository.findAll().stream()
            .map(p -> new EspecialidadPerfilResponseDto(
                p.getEspecialidad().getId(),
                p.getEspecialidad().getNombre(),
                p.getEspecialidad().getCampo(),
                p.getId(),
                p.getPrioCpu(),
                p.getPrioGpu(),
                p.getPrioDisco(),
                p.getPrioRam(),
                p.getPrioFuente(),
                p.getPrioPlaca(),
                p.getPrioCase(),
                p.getDescripcion()
            ))
            .toList();
    }
}
