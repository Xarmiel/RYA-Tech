package com.ecommerce.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompatibilidadCpuPlacaId implements Serializable {

    @Column(name = "cpu_id", nullable = false)
    private UUID cpuId;

    @Column(name = "placa_revision_id", nullable = false)
    private UUID placaRevisionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompatibilidadCpuPlacaId that = (CompatibilidadCpuPlacaId) o;
        return Objects.equals(cpuId, that.cpuId) && Objects.equals(placaRevisionId, that.placaRevisionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuId, placaRevisionId);
    }
}
