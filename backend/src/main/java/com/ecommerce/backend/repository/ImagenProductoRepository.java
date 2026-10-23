package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, UUID> {
    List<ImagenProducto> findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(UUID productoId);
    Optional<ImagenProducto> findFirstByProductoIdAndPrincipalTrueAndIsDeletedFalse(UUID productoId);
    List<ImagenProducto> findByProductoIdInAndIsDeletedFalseOrderByPosicionAsc(List<UUID> productoIds);
}
