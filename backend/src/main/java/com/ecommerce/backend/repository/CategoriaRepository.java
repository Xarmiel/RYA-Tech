package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    List<Categoria> findByIsDeletedFalseOrderByPosicionAsc();
    Optional<Categoria> findBySlugAndIsDeletedFalse(String slug);
    Optional<Categoria> findByAcronimoIgnoreCaseAndIsDeletedFalse(String acronimo);
    boolean existsBySlugAndIsDeletedFalse(String slug);
    boolean existsByNombreIgnoreCaseAndIsDeletedFalse(String nombre);
    boolean existsByAcronimoIgnoreCaseAndIsDeletedFalse(String acronimo);
}
