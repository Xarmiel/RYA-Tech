package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.CompatibilidadCpuPlaca;
import com.ecommerce.backend.entity.CompatibilidadCpuPlacaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompatibilidadCpuPlacaRepository extends JpaRepository<CompatibilidadCpuPlaca, CompatibilidadCpuPlacaId> {

    @Query("SELECT c FROM CompatibilidadCpuPlaca c " +
           "LEFT JOIN FETCH c.biosMinima " +
           "LEFT JOIN FETCH c.revisionPlaca " +
           "WHERE c.id.cpuId = :cpuId AND c.id.placaRevisionId = :placaRevisionId")
    Optional<CompatibilidadCpuPlaca> findConDetalles(@Param("cpuId") UUID cpuId, @Param("placaRevisionId") UUID placaRevisionId);

    @Query("SELECT c FROM CompatibilidadCpuPlaca c " +
           "LEFT JOIN FETCH c.biosMinima " +
           "LEFT JOIN FETCH c.revisionPlaca " +
           "WHERE c.id.cpuId = :cpuId AND c.soporte = 'SOPORTADO'")
    List<CompatibilidadCpuPlaca> findPlacasSoportadasPorCpuId(@Param("cpuId") UUID cpuId);
}
