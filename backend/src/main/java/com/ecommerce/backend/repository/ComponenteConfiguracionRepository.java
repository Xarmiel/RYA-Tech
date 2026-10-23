package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ComponenteConfiguracion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComponenteConfiguracionRepository extends JpaRepository<ComponenteConfiguracion, UUID> {

    @EntityGraph(attributePaths = {"tipoComponente", "producto", "producto.categoria", "producto.marca"})
    List<ComponenteConfiguracion> findByConfiguracionIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID configuracionId);

    @EntityGraph(attributePaths = {"tipoComponente", "producto"})
    Optional<ComponenteConfiguracion> findByConfiguracionIdAndTipoComponenteIdAndIsDeletedFalse(
            UUID configuracionId, UUID tipoComponenteId);
}
