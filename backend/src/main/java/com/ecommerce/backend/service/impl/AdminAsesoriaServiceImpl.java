package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.FamiliaPerfilAsesoriaDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.PerfilAsesoriaDto;
import com.ecommerce.backend.entity.FamiliaPerfilAsesoria;
import com.ecommerce.backend.entity.NecesidadPerfil;
import com.ecommerce.backend.entity.PerfilAsesoria;
import com.ecommerce.backend.entity.ReglaPresupuestoPerfil;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.FamiliaPerfilAsesoriaRepository;
import com.ecommerce.backend.repository.PerfilAsesoriaRepository;
import com.ecommerce.backend.service.AdminAsesoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAsesoriaServiceImpl implements AdminAsesoriaService {

    private final PerfilAsesoriaRepository perfilAsesoriaRepository;
    private final FamiliaPerfilAsesoriaRepository familiaPerfilAsesoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<PerfilAsesoriaDto> listarPerfiles(String busqueda, Boolean activo, UUID familiaId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombre"));
        String q = (busqueda != null && !busqueda.isBlank()) ? busqueda.trim() : null;
        Page<PerfilAsesoria> paged = perfilAsesoriaRepository.buscarAdmin(q, activo, familiaId, pageable);

        List<PerfilAsesoriaDto> content = paged.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PaginatedResponseDto.<PerfilAsesoriaDto>builder()
                .content(content)
                .page(paged.getNumber())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .last(paged.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilAsesoriaDto obtenerPerfilPorId(UUID id) {
        PerfilAsesoria perfil = perfilAsesoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asesoría no encontrado con id: " + id));
        return mapToDto(perfil);
    }

    @Override
    @Transactional
    public PerfilAsesoriaDto crearPerfil(PerfilAsesoriaDto dto) {
        if (dto.getCodigo() == null || dto.getCodigo().isBlank()) {
            throw new BadRequestException("El código del perfil es requerido.");
        }
        String codigoNorm = dto.getCodigo().trim().toUpperCase();
        if (perfilAsesoriaRepository.findByCodigoIgnoreCaseAndIsDeletedFalse(codigoNorm).isPresent()) {
            throw new BadRequestException("Ya existe un perfil con el código: " + codigoNorm);
        }

        FamiliaPerfilAsesoria familia = familiaPerfilAsesoriaRepository.findById(dto.getFamiliaId())
                .orElseThrow(() -> new ResourceNotFoundException("Familia de perfil no encontrada con id: " + dto.getFamiliaId()));

        PerfilAsesoria perfil = PerfilAsesoria.builder()
                .familia(familia)
                .codigo(codigoNorm)
                .nombre(dto.getNombre().trim())
                .descripcion(dto.getDescripcion())
                .prioridadCpu(dto.getPrioridadCpu() != null ? dto.getPrioridadCpu() : 5)
                .prioridadGpu(dto.getPrioridadGpu() != null ? dto.getPrioridadGpu() : 5)
                .ramMinimaGb(dto.getRamMinimaGb() != null ? dto.getRamMinimaGb() : 8)
                .requiereGpuDedicada(Boolean.TRUE.equals(dto.getRequiereGpuDedicada()))
                .presupuestoSugeridoMin(dto.getPresupuestoSugeridoMin() != null ? dto.getPresupuestoSugeridoMin() : BigDecimal.ZERO)
                .presupuestoSugeridoMax(dto.getPresupuestoSugeridoMax() != null ? dto.getPresupuestoSugeridoMax() : BigDecimal.ZERO)
                .activo(dto.getActivo() == null || dto.getActivo())
                .isDeleted(false)
                .necesidades(new ArrayList<>())
                .reglasPresupuesto(new ArrayList<>())
                .build();

        if (dto.getReglasPresupuesto() != null) {
            for (var r : dto.getReglasPresupuesto()) {
                BigDecimal min = r.getPorcentajeMin() != null ? r.getPorcentajeMin() : defaultMinParaPrioridad(r.getPrioridad());
                BigDecimal max = r.getPorcentajeMax() != null ? r.getPorcentajeMax() : defaultMaxParaPrioridad(r.getPrioridad());
                perfil.getReglasPresupuesto().add(ReglaPresupuestoPerfil.builder()
                        .perfil(perfil)
                        .tipoComponenteCodigo(r.getTipoComponenteCodigo().trim().toUpperCase())
                        .prioridad(r.getPrioridad())
                        .porcentajeMin(min)
                        .porcentajeMax(max)
                        .obligatorio(r.getObligatorio() == null || r.getObligatorio())
                        .build());
            }
        }

        if (dto.getNecesidades() != null) {
            int pos = 0;
            for (var n : dto.getNecesidades()) {
                perfil.getNecesidades().add(NecesidadPerfil.builder()
                        .perfil(perfil)
                        .codigo(n.getCodigo() != null ? n.getCodigo().trim() : null)
                        .nombre(n.getNombre().trim())
                        .descripcion(n.getDescripcion())
                        .posicion(pos++)
                        .activo(true)
                        .build());
            }
        }

        PerfilAsesoria guardado = perfilAsesoriaRepository.save(perfil);
        return mapToDto(guardado);
    }

    @Override
    @Transactional
    public PerfilAsesoriaDto actualizarPerfil(UUID id, PerfilAsesoriaDto dto) {
        PerfilAsesoria perfil = perfilAsesoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asesoría no encontrado con id: " + id));

        if (dto.getCodigo() != null && !dto.getCodigo().isBlank()) {
            String codigoNorm = dto.getCodigo().trim().toUpperCase();
            var exist = perfilAsesoriaRepository.findByCodigoIgnoreCaseAndIsDeletedFalse(codigoNorm);
            if (exist.isPresent() && !exist.get().getId().equals(id)) {
                throw new BadRequestException("Ya existe otro perfil con el código: " + codigoNorm);
            }
            perfil.setCodigo(codigoNorm);
        }

        if (dto.getFamiliaId() != null && !dto.getFamiliaId().equals(perfil.getFamilia().getId())) {
            FamiliaPerfilAsesoria nuevaFamilia = familiaPerfilAsesoriaRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada: " + dto.getFamiliaId()));
            perfil.setFamilia(nuevaFamilia);
        }

        if (dto.getNombre() != null) perfil.setNombre(dto.getNombre().trim());
        if (dto.getDescripcion() != null) perfil.setDescripcion(dto.getDescripcion());
        if (dto.getPrioridadCpu() != null) perfil.setPrioridadCpu(dto.getPrioridadCpu());
        if (dto.getPrioridadGpu() != null) perfil.setPrioridadGpu(dto.getPrioridadGpu());
        if (dto.getRamMinimaGb() != null) perfil.setRamMinimaGb(dto.getRamMinimaGb());
        if (dto.getRequiereGpuDedicada() != null) perfil.setRequiereGpuDedicada(dto.getRequiereGpuDedicada());
        if (dto.getPresupuestoSugeridoMin() != null) perfil.setPresupuestoSugeridoMin(dto.getPresupuestoSugeridoMin());
        if (dto.getPresupuestoSugeridoMax() != null) perfil.setPresupuestoSugeridoMax(dto.getPresupuestoSugeridoMax());
        if (dto.getActivo() != null) perfil.setActivo(dto.getActivo());

        if (dto.getReglasPresupuesto() != null) {
            perfil.getReglasPresupuesto().clear();
            for (var r : dto.getReglasPresupuesto()) {
                BigDecimal min = r.getPorcentajeMin() != null ? r.getPorcentajeMin() : defaultMinParaPrioridad(r.getPrioridad());
                BigDecimal max = r.getPorcentajeMax() != null ? r.getPorcentajeMax() : defaultMaxParaPrioridad(r.getPrioridad());
                perfil.getReglasPresupuesto().add(ReglaPresupuestoPerfil.builder()
                        .perfil(perfil)
                        .tipoComponenteCodigo(r.getTipoComponenteCodigo().trim().toUpperCase())
                        .prioridad(r.getPrioridad())
                        .porcentajeMin(min)
                        .porcentajeMax(max)
                        .obligatorio(r.getObligatorio() == null || r.getObligatorio())
                        .build());
            }
        }

        if (dto.getNecesidades() != null) {
            perfil.getNecesidades().clear();
            int pos = 0;
            for (var n : dto.getNecesidades()) {
                perfil.getNecesidades().add(NecesidadPerfil.builder()
                        .perfil(perfil)
                        .codigo(n.getCodigo() != null ? n.getCodigo().trim() : null)
                        .nombre(n.getNombre().trim())
                        .descripcion(n.getDescripcion())
                        .posicion(pos++)
                        .activo(true)
                        .build());
            }
        }

        return mapToDto(perfilAsesoriaRepository.save(perfil));
    }

    @Override
    @Transactional
    public PerfilAsesoriaDto cambiarEstado(UUID id, boolean activo) {
        PerfilAsesoria perfil = perfilAsesoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asesoría no encontrado con id: " + id));
        perfil.setActivo(activo);
        return mapToDto(perfilAsesoriaRepository.save(perfil));
    }

    @Override
    @Transactional
    public void eliminarPerfil(UUID id) {
        PerfilAsesoria perfil = perfilAsesoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de asesoría no encontrado con id: " + id));
        perfil.setIsDeleted(true);
        perfil.setActivo(false);
        perfilAsesoriaRepository.save(perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamiliaPerfilAsesoriaDto> listarFamilias() {
        return familiaPerfilAsesoriaRepository.findByIsDeletedFalseOrderByPosicionAsc().stream()
                .map(f -> FamiliaPerfilAsesoriaDto.builder()
                        .id(f.getId())
                        .codigo(f.getCodigo())
                        .nombre(f.getNombre())
                        .descripcion(f.getDescripcion())
                        .icono(f.getIcono())
                        .posicion(f.getPosicion())
                        .activo(f.getActivo())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public FamiliaPerfilAsesoriaDto crearFamilia(FamiliaPerfilAsesoriaDto dto) {
        if (dto.getCodigo() == null || dto.getCodigo().isBlank()) {
            throw new BadRequestException("El código de la familia es requerido.");
        }
        String codigoNorm = dto.getCodigo().trim().toUpperCase();
        if (familiaPerfilAsesoriaRepository.findByCodigoIgnoreCaseAndIsDeletedFalse(codigoNorm).isPresent()) {
            throw new BadRequestException("Ya existe una familia con el código: " + codigoNorm);
        }

        FamiliaPerfilAsesoria familia = FamiliaPerfilAsesoria.builder()
                .codigo(codigoNorm)
                .nombre(dto.getNombre().trim())
                .descripcion(dto.getDescripcion())
                .icono(dto.getIcono() != null && !dto.getIcono().isBlank() ? dto.getIcono() : "tune")
                .posicion(dto.getPosicion() != null ? dto.getPosicion() : 0)
                .activo(dto.getActivo() == null || dto.getActivo())
                .isDeleted(false)
                .build();

        FamiliaPerfilAsesoria guardada = familiaPerfilAsesoriaRepository.save(familia);
        return FamiliaPerfilAsesoriaDto.builder()
                .id(guardada.getId())
                .codigo(guardada.getCodigo())
                .nombre(guardada.getNombre())
                .descripcion(guardada.getDescripcion())
                .icono(guardada.getIcono())
                .posicion(guardada.getPosicion())
                .activo(guardada.getActivo())
                .build();
    }

    private PerfilAsesoriaDto mapToDto(PerfilAsesoria p) {
        return PerfilAsesoriaDto.builder()
                .id(p.getId())
                .familiaId(p.getFamilia() != null ? p.getFamilia().getId() : null)
                .familiaNombre(p.getFamilia() != null ? p.getFamilia().getNombre() : null)
                .familiaCodigo(p.getFamilia() != null ? p.getFamilia().getCodigo() : null)
                .codigo(p.getCodigo())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .prioridadCpu(p.getPrioridadCpu())
                .prioridadGpu(p.getPrioridadGpu())
                .ramMinimaGb(p.getRamMinimaGb())
                .requiereGpuDedicada(p.getRequiereGpuDedicada())
                .presupuestoSugeridoMin(p.getPresupuestoSugeridoMin())
                .presupuestoSugeridoMax(p.getPresupuestoSugeridoMax())
                .activo(p.getActivo())
                .reglasPresupuesto(p.getReglasPresupuesto() != null ? p.getReglasPresupuesto().stream()
                        .map(r -> PerfilAsesoriaDto.ReglaPresupuestoDto.builder()
                                .id(r.getId())
                                .tipoComponenteCodigo(r.getTipoComponenteCodigo())
                                .prioridad(r.getPrioridad())
                                .porcentajeMin(r.getPorcentajeMin())
                                .porcentajeMax(r.getPorcentajeMax())
                                .obligatorio(r.getObligatorio())
                                .build())
                        .toList() : List.of())
                .necesidades(p.getNecesidades() != null ? p.getNecesidades().stream()
                        .map(n -> PerfilAsesoriaDto.NecesidadPerfilDto.builder()
                                .id(n.getId())
                                .codigo(n.getCodigo())
                                .nombre(n.getNombre())
                                .descripcion(n.getDescripcion())
                                .build())
                        .toList() : List.of())
                .build();
    }

    private BigDecimal defaultMinParaPrioridad(Integer prioridad) {
        if (prioridad == null) return BigDecimal.ZERO;
        return switch (prioridad) {
            case 1 -> new BigDecimal("35.00");
            case 2 -> new BigDecimal("20.00");
            case 3 -> new BigDecimal("12.00");
            case 4 -> new BigDecimal("10.00");
            case 5 -> new BigDecimal("8.00");
            case 6 -> new BigDecimal("5.00");
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal defaultMaxParaPrioridad(Integer prioridad) {
        if (prioridad == null) return BigDecimal.ZERO;
        return switch (prioridad) {
            case 1 -> new BigDecimal("45.00");
            case 2 -> new BigDecimal("30.00");
            case 3 -> new BigDecimal("18.00");
            case 4 -> new BigDecimal("15.00");
            case 5 -> new BigDecimal("12.00");
            case 6 -> new BigDecimal("10.00");
            default -> BigDecimal.ZERO;
        };
    }
}
