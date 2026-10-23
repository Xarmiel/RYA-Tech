package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

/** Serializes inventory operations, including the first stock row and retries. */
public interface InventarioProductLockRepository extends Repository<Producto, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id=:id AND p.isDeleted=false")
    Optional<Producto> bloquear(@Param("id") UUID id);
}
