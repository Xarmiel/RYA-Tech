package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    @org.springframework.data.jpa.repository.Query("select f from CampoFichaCategoria f where f.categoriaId=:categoria order by f.createdAt,f.id")
    List<com.ecommerce.backend.entity.CampoFichaCategoria> findTechnicalFields(@org.springframework.data.repository.query.Param("categoria") UUID categoria);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Categoria c where c.id=:id and c.isDeleted=false")
    Optional<Categoria> findForTechnicalFieldUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    List<Categoria> findByIsDeletedFalseOrderByPosicionAsc();
    Optional<Categoria> findBySlugAndIsDeletedFalse(String slug);
    Optional<Categoria> findByAcronimoIgnoreCaseAndIsDeletedFalse(String acronimo);
    boolean existsBySlugAndIsDeletedFalse(String slug);
    boolean existsByNombreIgnoreCaseAndIsDeletedFalse(String nombre);
    boolean existsByParentIdAndIsDeletedFalse(UUID parentId);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsBySlug(String slug);
    boolean existsByAcronimoIgnoreCaseAndIsDeletedFalse(String acronimo);
}
