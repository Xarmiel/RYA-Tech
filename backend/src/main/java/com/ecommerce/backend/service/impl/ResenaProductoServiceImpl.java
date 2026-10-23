package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.ResenaProductoDto;
import com.ecommerce.backend.dto.ResenasProductoDto;
import com.ecommerce.backend.repository.ResenaProductoRepository;
import com.ecommerce.backend.service.ResenaProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResenaProductoServiceImpl implements ResenaProductoService {
    private final ResenaProductoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ResenasProductoDto listarPublicadas(UUID productoId) {
        List<ResenaProductoDto> resenas = repository.findPublicadasByProductoId(productoId).stream()
                .map(item -> ResenaProductoDto.builder()
                        .id(item.getId())
                        .calificacion(item.getCalificacion())
                        .comentario(item.getComentario())
                        .autor(item.getAutor())
                        .fecha(item.getFecha())
                        .build())
                .toList();
        BigDecimal promedio = resenas.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(resenas.stream().mapToInt(ResenaProductoDto::getCalificacion).average().orElse(0))
                        .setScale(1, RoundingMode.HALF_UP);
        return ResenasProductoDto.builder().promedio(promedio).total(resenas.size()).resenas(resenas).build();
    }
}
