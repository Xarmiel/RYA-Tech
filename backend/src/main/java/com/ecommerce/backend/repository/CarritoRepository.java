package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CarritoRepository extends JpaRepository<Carrito, UUID> {
    Optional<Carrito> findFirstBySessionKeyHashAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(
            String sessionKeyHash, String estado);

    Optional<Carrito> findFirstByUsuarioIdAndEstadoAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID usuarioId, String estado);
}
