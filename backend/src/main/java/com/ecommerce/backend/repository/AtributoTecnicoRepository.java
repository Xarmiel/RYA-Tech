package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.AtributoTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AtributoTecnicoRepository extends JpaRepository<AtributoTecnico, UUID> {
    List<AtributoTecnico> findByFiltrableTrueAndIsDeletedFalseOrderByNombreAsc();
    Optional<AtributoTecnico> findByCodigoAndIsDeletedFalse(String codigo);
}
