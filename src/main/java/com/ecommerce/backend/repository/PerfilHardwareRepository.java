package com.ecommerce.backend.repository;

import com.ecommerce.backend.model.PerfilHardware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilHardwareRepository extends JpaRepository<PerfilHardware, Integer> {
    Optional<PerfilHardware> findByEspecialidadId(Integer especialidadId);
}
