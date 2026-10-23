package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.CategoriaDto;
import com.ecommerce.backend.entity.Categoria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoriaMapper {

    public CategoriaDto toDto(Categoria entity) {
        if (entity == null) {
            return null;
        }
        return CategoriaDto.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .nombre(entity.getNombre())
                .slug(entity.getSlug())
                .acronimo(entity.getAcronimo())
                .posicion(entity.getPosicion())
                .build();
    }

    public List<CategoriaDto> toDtoList(List<Categoria> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toDto).toList();
    }
}
