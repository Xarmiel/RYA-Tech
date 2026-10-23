package com.ecommerce.backend.technicaldraft;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="borradores_tecnicos_eventos",schema="rya")
@Getter @Setter
public class TechnicalDraftEvent {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="borrador_id",nullable=false,updatable=false) private UUID borradorId;
    @Column(nullable=false,updatable=false) private Long version;
    @Column(nullable=false,updatable=false) private UUID actor;
    @Column(nullable=false,updatable=false,length=30) private String accion;
    @Column(nullable=false,updatable=false,length=20) private String estado;
    @Column(nullable=false,updatable=false,length=2000) private String comentario;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @Column(name="document_json",nullable=false,updatable=false,columnDefinition="text") private String documentJson;
}
