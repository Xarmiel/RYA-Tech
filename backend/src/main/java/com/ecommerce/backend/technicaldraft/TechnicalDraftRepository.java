package com.ecommerce.backend.technicaldraft;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface TechnicalDraftRepository extends JpaRepository<TechnicalDraft,UUID>, JpaSpecificationExecutor<TechnicalDraft> {}
