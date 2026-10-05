package com.ecommerce.backend.repository;

import com.ecommerce.backend.model.Producto;
import com.ecommerce.backend.model.enums.TipoProductoEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    Optional<Producto> findBySku(String sku);

    @EntityGraph(attributePaths = {"especificaciones", "categoria"})
    List<Producto> findByActivoTrue();

    @EntityGraph(attributePaths = {"especificaciones", "categoria"})
    List<Producto> findByCategoriaId(Integer categoriaId);

    List<Producto> findByTipoProducto(TipoProductoEnum tipoProducto);

    Optional<Producto> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    @Query("SELECT p FROM Producto p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) AND p.activo = true")
    List<Producto> buscarPorNombreContiene(@Param("query") String query);

    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.especificaciones LEFT JOIN FETCH p.categoria WHERE p.id = :id")
    Optional<Producto> findByIdWithEspecificaciones(@Param("id") UUID id);
}