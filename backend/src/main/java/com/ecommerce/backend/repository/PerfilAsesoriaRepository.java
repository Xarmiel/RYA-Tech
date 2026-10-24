package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.PerfilAsesoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerfilAsesoriaRepository extends JpaRepository<PerfilAsesoria, UUID> {

    List<PerfilAsesoria> findByFamiliaIdAndIsDeletedFalse(UUID familiaId);

    List<PerfilAsesoria> findByActivoTrueAndIsDeletedFalse();

    Optional<PerfilAsesoria> findByCodigoIgnoreCaseAndIsDeletedFalse(String codigo);

    @Query(value = "SELECT p FROM PerfilAsesoria p LEFT JOIN FETCH p.familia f " +
           "WHERE p.isDeleted = false " +
           "AND (:busqueda IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) OR LOWER(f.nombre) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))) " +
           "AND (:activo IS NULL OR p.activo = :activo) " +
           "AND (:familiaId IS NULL OR f.id = :familiaId)",
           countQuery = "SELECT COUNT(p) FROM PerfilAsesoria p LEFT JOIN p.familia f " +
           "WHERE p.isDeleted = false " +
           "AND (:busqueda IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) OR LOWER(f.nombre) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))) " +
           "AND (:activo IS NULL OR p.activo = :activo) " +
           "AND (:familiaId IS NULL OR f.id = :familiaId)")
    Page<PerfilAsesoria> buscarAdmin(
            @Param("busqueda") String busqueda,
            @Param("activo") Boolean activo,
            @Param("familiaId") UUID familiaId,
            Pageable pageable
    );
}
