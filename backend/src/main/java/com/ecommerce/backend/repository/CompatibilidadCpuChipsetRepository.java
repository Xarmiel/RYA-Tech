package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.CompatibilidadCpuChipset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompatibilidadCpuChipsetRepository extends JpaRepository<CompatibilidadCpuChipset, UUID> {

    List<CompatibilidadCpuChipset> findBySocketIgnoreCase(String socket);

    Optional<CompatibilidadCpuChipset> findFirstBySocketIgnoreCaseAndGeneracionIgnoreCaseAndChipsetIgnoreCase(
            String socket, String generacion, String chipset);
}
