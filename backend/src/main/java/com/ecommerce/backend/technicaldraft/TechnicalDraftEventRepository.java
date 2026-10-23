package com.ecommerce.backend.technicaldraft;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.Optional;

public interface TechnicalDraftEventRepository extends JpaRepository<TechnicalDraftEvent,UUID> {
    Page<TechnicalDraftEvent> findByBorradorId(UUID id, Pageable page);
    Optional<TechnicalDraftEvent> findByBorradorIdAndVersion(UUID id,Long version);
}
