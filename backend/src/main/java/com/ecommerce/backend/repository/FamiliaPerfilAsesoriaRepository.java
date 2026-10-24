package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.FamiliaPerfilAsesoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamiliaPerfilAsesoriaRepository extends JpaRepository<FamiliaPerfilAsesoria, UUID> {
    List<FamiliaPerfilAsesoria> findByIsDeletedFalseOrderByPosicionAsc();
    List<FamiliaPerfilAsesoria> findByActivoTrueAndIsDeletedFalseOrderByPosicionAsc();
    Optional<FamiliaPerfilAsesoria> findByCodigoIgnoreCaseAndIsDeletedFalse(String codigo);
}
