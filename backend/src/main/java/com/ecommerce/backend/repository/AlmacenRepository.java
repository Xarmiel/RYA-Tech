package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, UUID> {

    List<Almacen> findByActivoTrueAndIsDeletedFalseOrderByNombreAsc();

    Optional<Almacen> findByCodigoAndIsDeletedFalse(String codigo);

    Optional<Almacen> findFirstByActivoTrueAndIsDeletedFalse();
}
