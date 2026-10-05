package com.ecommerce.backend.repository;

import com.ecommerce.backend.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {
    Optional<Especialidad> findByNombreIgnoreCase(String nombre);
}
