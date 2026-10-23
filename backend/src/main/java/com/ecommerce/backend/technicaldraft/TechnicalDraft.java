package com.ecommerce.backend.technicaldraft;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "borradores_tecnicos", schema = "rya")
@Getter @Setter
public class TechnicalDraft {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Version private Long version;
    @Column(name="created_by",nullable=false,updatable=false) private UUID createdBy;
    @Column(name="updated_by",nullable=false) private UUID updatedBy;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Column(nullable=false,length=80) private String sku;
    @Column(nullable=false,length=220) private String nombre;
    @Column(nullable=false,length=30) private String tipo;
    @Column(nullable=false,length=20) private String estado;
    @Column(name="document_json",nullable=false,columnDefinition="text") private String documentJson;
}
