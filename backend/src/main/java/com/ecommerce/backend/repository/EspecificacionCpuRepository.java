package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.EspecificacionCpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EspecificacionCpuRepository extends JpaRepository<EspecificacionCpu, UUID> {

    @Query("SELECT c FROM EspecificacionCpu c JOIN FETCH c.producto p LEFT JOIN FETCH p.marca WHERE c.productoId = :productoId AND p.isDeleted = false")
    Optional<EspecificacionCpu> findConProductoPorId(@Param("productoId") UUID productoId);

    @Query("SELECT c FROM EspecificacionCpu c JOIN FETCH c.producto p LEFT JOIN FETCH p.marca WHERE p.activo = true AND p.vendible = true AND p.isDeleted = false")
    List<EspecificacionCpu> findAllActivos();

    @Query("SELECT c FROM EspecificacionCpu c JOIN FETCH c.producto p LEFT JOIN FETCH p.marca WHERE c.socket = :socket AND c.productoId <> :excludeId AND p.activo = true AND p.vendible = true AND p.isDeleted = false")
    List<EspecificacionCpu> findAlternativasPorSocket(@Param("socket") String socket, @Param("excludeId") UUID excludeId);

    @Query("SELECT c FROM EspecificacionCpu c JOIN FETCH c.producto p LEFT JOIN FETCH p.marca WHERE c.familia = :familia AND c.productoId <> :excludeId AND p.activo = true AND p.vendible = true AND p.isDeleted = false")
    List<EspecificacionCpu> findAlternativasPorFamilia(@Param("familia") String familia, @Param("excludeId") UUID excludeId);
}
