package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ValorAtributoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ValorAtributoProductoRepository extends JpaRepository<ValorAtributoProducto, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "atributo")
    List<ValorAtributoProducto> findAllByProducto_Id(UUID productoId);

    @Query("SELECT v FROM ValorAtributoProducto v LEFT JOIN FETCH v.atributo WHERE v.producto.id = :productoId AND v.isDeleted = false")
    List<ValorAtributoProducto> findConAtributoPorProductoId(@Param("productoId") UUID productoId);

    @Query("SELECT v FROM ValorAtributoProducto v LEFT JOIN FETCH v.atributo " +
            "WHERE v.producto.id IN :productoIds AND v.isDeleted = false AND v.atributo.isDeleted = false")
    List<ValorAtributoProducto> findConAtributoPorProductoIds(@Param("productoIds") List<UUID> productoIds);

    @Query("SELECT DISTINCT v.valorTexto FROM ValorAtributoProducto v " +
           "JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId " +
           "AND v.isDeleted = false " +
           "AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorTexto IS NOT NULL AND TRIM(v.valorTexto) <> '' " +
           "ORDER BY v.valorTexto ASC")
    List<String> findValoresTextoDistintos(@Param("atributoId") UUID atributoId);

    @Query("SELECT DISTINCT CAST(v.valorNumero AS string) FROM ValorAtributoProducto v " +
           "JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId " +
           "AND v.isDeleted = false " +
           "AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorNumero IS NOT NULL " +
           "ORDER BY CAST(v.valorNumero AS string) ASC")
    List<String> findValoresNumeroDistintos(@Param("atributoId") UUID atributoId);

    @Query("SELECT DISTINCT CAST(v.valorBooleano AS string) FROM ValorAtributoProducto v " +
           "JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId " +
           "AND v.isDeleted = false " +
           "AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorBooleano IS NOT NULL")
    List<String> findValoresBooleanoDistintos(@Param("atributoId") UUID atributoId);

    @Query("SELECT DISTINCT v.valorTexto FROM ValorAtributoProducto v JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId AND p.categoria.id = :categoriaId " +
           "AND v.isDeleted = false AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorTexto IS NOT NULL AND TRIM(v.valorTexto) <> '' ORDER BY v.valorTexto ASC")
    List<String> findValoresTextoDistintosPorCategoria(@Param("atributoId") UUID atributoId,
                                                        @Param("categoriaId") UUID categoriaId);

    @Query("SELECT DISTINCT CAST(v.valorNumero AS string) FROM ValorAtributoProducto v JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId AND p.categoria.id = :categoriaId " +
           "AND v.isDeleted = false AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorNumero IS NOT NULL ORDER BY CAST(v.valorNumero AS string) ASC")
    List<String> findValoresNumeroDistintosPorCategoria(@Param("atributoId") UUID atributoId,
                                                         @Param("categoriaId") UUID categoriaId);

    @Query("SELECT DISTINCT CAST(v.valorBooleano AS string) FROM ValorAtributoProducto v JOIN v.producto p " +
           "WHERE v.atributo.id = :atributoId AND p.categoria.id = :categoriaId " +
           "AND v.isDeleted = false AND p.activo = true AND p.vendible = true AND p.isDeleted = false " +
           "AND v.valorBooleano IS NOT NULL")
    List<String> findValoresBooleanoDistintosPorCategoria(@Param("atributoId") UUID atributoId,
                                                           @Param("categoriaId") UUID categoriaId);
}
