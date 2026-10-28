package com.ecommerce.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name="clientes_pago",schema="rya",uniqueConstraints=@UniqueConstraint(columnNames={"usuario_id","entorno"}))
@Getter @Setter @NoArgsConstructor
public class ClientePago {
 @Id private UUID id;
 @Column(name="usuario_id",nullable=false) private UUID usuarioId;
 @Column(nullable=false,length=80) private String entorno;
 @Column(name="customer_id",length=100) private String customerId;
}
