package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.TipoComponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoComponenteRepository extends JpaRepository<TipoComponente, UUID> {
    List<TipoComponente> findByIsDeletedFalseOrderByCodigoAsc();
    Optional<TipoComponente> findByCodigoIgnoreCaseAndIsDeletedFalse(String codigo);
}
