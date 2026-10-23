package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ConfiguracionPc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ConfiguracionPcRepository extends JpaRepository<ConfiguracionPc, UUID> {
    Optional<ConfiguracionPc> findByIdAndIsDeletedFalse(UUID id);
    Optional<ConfiguracionPc> findByIdAndUsuarioIdAndIsDeletedFalse(UUID id, UUID usuarioId);
    List<ConfiguracionPc> findByUsuarioIdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID usuarioId);
}
