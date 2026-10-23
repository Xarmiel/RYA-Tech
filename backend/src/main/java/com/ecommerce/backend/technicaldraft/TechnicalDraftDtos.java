package com.ecommerce.backend.technicaldraft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TechnicalDraftDtos {
    private TechnicalDraftDtos() {}
    public record Create(@NotNull JsonNode document) {}
    public record Update(@NotNull @PositiveOrZero @JsonDeserialize(using=DraftVersionDeserializer.class) Long version, @NotNull JsonNode document) {}
    public record Transition(@NotNull @PositiveOrZero @JsonDeserialize(using=DraftVersionDeserializer.class) Long version, @Size(max=2000) String comment) {}
    public record Decision(@NotNull @PositiveOrZero @JsonDeserialize(using=DraftVersionDeserializer.class) Long version, @NotNull String decision, @Size(min=1,max=2000) String comment) {}
    public record Validation(boolean valid, List<String> issues, List<String> warnings, JsonNode document) {}
    public record Detail(UUID id, Long version, String state, String verificationStatus, UUID createdBy, UUID updatedBy,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt, JsonNode document) {}
    public record Summary(UUID id, Long version, String name, String sku, String type, String state, UUID createdBy, OffsetDateTime updatedAt) {}
    public record Event(UUID id, Long version, UUID actor, String action, String state, String comment, OffsetDateTime createdAt) {}
    public record Revision(Event event, String verificationStatus, JsonNode document) {}
}
