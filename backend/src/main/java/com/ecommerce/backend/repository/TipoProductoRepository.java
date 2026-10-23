package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoProductoRepository extends JpaRepository<TipoProducto, UUID> {
    Optional<TipoProducto> findByCodigoAndIsDeletedFalse(String codigo);
}
