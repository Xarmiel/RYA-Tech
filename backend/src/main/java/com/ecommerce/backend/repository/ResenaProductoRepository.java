package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.ResenaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ResenaProductoRepository extends JpaRepository<ResenaProducto, UUID> {
    @Query(value = """
            SELECT r.id AS id,
                   r.calificacion AS calificacion,
                   r.comentario AS comentario,
                   COALESCE(NULLIF(TRIM(u.nombre), ''), 'Cliente verificado') AS autor,
                   r.created_at AS fecha
              FROM rya.resenas_producto r
              JOIN rya.usuarios u ON u.id = r.usuario_id
             WHERE r.producto_id = :productoId
               AND r.publicada = true
               AND r.is_deleted = false
               AND u.is_deleted = false
             ORDER BY r.created_at DESC
            """, nativeQuery = true)
    List<ResenaPublicadaProjection> findPublicadasByProductoId(@Param("productoId") UUID productoId);

    interface ResenaPublicadaProjection {
        UUID getId();
        Short getCalificacion();
        String getComentario();
        String getAutor();
        OffsetDateTime getFecha();
    }
}
