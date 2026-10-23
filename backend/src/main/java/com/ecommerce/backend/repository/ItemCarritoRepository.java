package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ItemCarrito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, UUID> {
    @EntityGraph(attributePaths = {"producto", "producto.marca", "producto.categoria"})
    List<ItemCarrito> findByCarritoIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID carritoId);

    Optional<ItemCarrito> findByCarritoIdAndProductoIdAndIsDeletedFalse(UUID carritoId, UUID productoId);

    @EntityGraph(attributePaths = {"carrito", "producto"})
    Optional<ItemCarrito> findByIdAndCarritoIdAndIsDeletedFalse(UUID id, UUID carritoId);
}
