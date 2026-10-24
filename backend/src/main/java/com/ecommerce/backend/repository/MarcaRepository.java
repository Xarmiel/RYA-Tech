package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, UUID> {
    List<Marca> findByIsDeletedFalseOrderByNombreAsc();
    Optional<Marca> findBySlugAndIsDeletedFalse(String slug);
    boolean existsBySlugAndIsDeletedFalse(String slug);
    boolean existsByNombreIgnoreCaseAndIsDeletedFalse(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsBySlug(String slug);
}
