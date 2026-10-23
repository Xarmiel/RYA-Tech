package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.TarifaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TarifaEnvioRepository extends JpaRepository<TarifaEnvio, UUID> {
    @Query(value = """
            SELECT t.id AS id, tr.nombre AS transportista, t.departamento AS departamento,
                   t.provincia AS provincia, t.distrito AS distrito, t.costo AS costo,
                   t.plazo_dias AS plazoDias
              FROM rya.tarifas_envio t
              JOIN rya.transportistas tr ON tr.id = t.transportista_id
             WHERE t.is_deleted = false AND tr.is_deleted = false
             ORDER BY t.departamento NULLS LAST, t.provincia NULLS LAST, t.distrito NULLS LAST, t.costo
            """, nativeQuery = true)
    List<TarifaDisponibleProjection> findDisponibles();

    Optional<TarifaEnvio> findByIdAndIsDeletedFalse(UUID id);

    interface TarifaDisponibleProjection {
        UUID getId();
        String getTransportista();
        String getDepartamento();
        String getProvincia();
        String getDistrito();
        BigDecimal getCosto();
        Integer getPlazoDias();
    }
}
