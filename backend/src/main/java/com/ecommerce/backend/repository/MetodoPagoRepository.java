package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, UUID> {
    List<MetodoPago> findByActivoTrueAndIsDeletedFalseOrderByNombreAsc();
    Optional<MetodoPago> findByIdAndActivoTrueAndIsDeletedFalse(UUID id);
}
