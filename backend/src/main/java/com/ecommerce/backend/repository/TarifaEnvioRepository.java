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
            SELECT CAST(t.id AS varchar) AS id, COALESCE(t.nombre_servicio, tr.nombre) AS transportista, t.departamento AS departamento,
                   t.provincia AS provincia, t.distrito AS distrito, t.costo AS costo,
                   t.plazo_dias AS plazoDias, t.modalidad AS modalidad,
                   t.direccion_recojo AS direccionRecojo, t.horario_recojo AS horarioRecojo
              FROM rya.tarifas_envio t
              LEFT JOIN rya.transportistas tr ON tr.id = t.transportista_id
             WHERE t.is_deleted = false AND t.activo = true AND (t.transportista_id IS NULL OR tr.is_deleted = false)
             ORDER BY t.departamento NULLS LAST, t.provincia NULLS LAST, t.distrito NULLS LAST, t.costo
            """, nativeQuery = true)
    List<TarifaDisponibleProjection> findDisponibles();

    @Query(value = "SELECT t.* FROM rya.tarifas_envio t LEFT JOIN rya.transportistas tr ON tr.id=t.transportista_id WHERE t.id=:id AND t.is_deleted=false AND t.activo=true AND (t.transportista_id IS NULL OR tr.is_deleted=false)", nativeQuery = true)
    Optional<TarifaEnvio> findByIdAndIsDeletedFalse(UUID id);
    List<TarifaEnvio> findByIsDeletedFalseOrderByDepartamentoAsc();

    interface TarifaDisponibleProjection {
        String getModalidad();
        String getDireccionRecojo();
        String getHorarioRecojo();
        UUID getId();
        String getTransportista();
        String getDepartamento();
        String getProvincia();
        String getDistrito();
        BigDecimal getCosto();
        Integer getPlazoDias();
    }
}
