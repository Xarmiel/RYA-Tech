package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.VersionBios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionBiosRepository extends JpaRepository<VersionBios, UUID> {
    List<VersionBios> findByPlacaRevisionIdOrderByOrdenAsc(UUID placaRevisionId);
    Optional<VersionBios> findByPlacaRevisionIdAndVersionIgnoreCase(UUID placaRevisionId, String version);
}
