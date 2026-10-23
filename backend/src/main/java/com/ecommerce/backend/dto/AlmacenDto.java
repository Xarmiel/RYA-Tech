package com.ecommerce.backend.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlmacenDto {
    private UUID id;
    private String nombre;
    private String codigo;
    private String direccion;
    private Boolean activo;
}
