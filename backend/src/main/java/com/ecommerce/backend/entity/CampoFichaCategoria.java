package com.ecommerce.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
@Entity @Table(name="campos_ficha_categoria",schema="rya") @Getter @Setter @NoArgsConstructor
public class CampoFichaCategoria {
 @Id private UUID id;
 @Column(name="categoria_id",nullable=false) private UUID categoriaId;
 @Column(nullable=false,unique=true,length=40) private String clave;
 @Column(nullable=false,length=100) private String nombre;
 @Column(name="tipo_dato",nullable=false,length=10) private String tipoDato;
 @Column(length=20) private String unidad;
 @Column(length=120) private String ejemplo;
 @Column(name="solo_enteros",nullable=false) private boolean soloEnteros;
 @Version @Column(nullable=false) private Long version;
 @Column(name="creado_por",nullable=false) private UUID creadoPor;
 @Column(name="actualizado_por",nullable=false) private UUID actualizadoPor;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
}
