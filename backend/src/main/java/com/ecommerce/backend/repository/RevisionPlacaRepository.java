package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.RevisionPlaca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RevisionPlacaRepository extends JpaRepository<RevisionPlaca, UUID> {
    List<RevisionPlaca> findByProductoId(UUID productoId);
    List<RevisionPlaca> findBySocketIgnoreCase(String socket);
}
