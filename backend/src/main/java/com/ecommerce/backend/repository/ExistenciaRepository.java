package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Existencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExistenciaRepository extends JpaRepository<Existencia, UUID>, JpaSpecificationExecutor<Existencia> {

    @Query("SELECT COALESCE(SUM(e.cantidad - e.reservado), 0) FROM Existencia e WHERE e.producto.id = :productoId AND e.isDeleted = false")
    Integer findStockDisponiblePorProductoId(@Param("productoId") UUID productoId);

    @Query("SELECT e.producto.id, COALESCE(SUM(e.cantidad - e.reservado), 0) FROM Existencia e WHERE e.producto.id IN :productoIds AND e.isDeleted = false GROUP BY e.producto.id")
    List<Object[]> findStockDisponiblePorProductoIds(@Param("productoIds") List<UUID> productoIds);

    List<Existencia> findByProductoIdAndIsDeletedFalse(UUID productoId);

    Optional<Existencia> findByProductoIdAndAlmacenIdAndIsDeletedFalse(UUID productoId, UUID almacenId);

    @Query("SELECT e FROM Existencia e WHERE e.producto.id=:producto AND e.almacenId=:almacen AND e.isDeleted=false AND ((:variante IS NULL AND e.varianteId IS NULL) OR e.varianteId=:variante)")
    List<Existencia> buscarExacta(@Param("producto") UUID producto, @Param("almacen") UUID almacen, @Param("variante") UUID variante);

    Optional<Existencia> findFirstByIsDeletedFalse();
}
