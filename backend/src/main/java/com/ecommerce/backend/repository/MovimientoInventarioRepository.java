package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, UUID> {

    List<MovimientoInventario> findByExistenciaIdOrderByRegistradoAtDesc(UUID existenciaId);
    Page<MovimientoInventario> findByExistenciaId(UUID existenciaId, Pageable pageable);
    Optional<MovimientoInventario> findByReferenciaTipoAndReferenciaIdAndExistenciaProductoId(String tipo, UUID referencia, UUID producto);
}
