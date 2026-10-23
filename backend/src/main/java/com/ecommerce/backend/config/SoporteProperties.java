package com.ecommerce.backend.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix="soporte")
@Validated
@Getter @Setter
public class SoporteProperties {
    @Email private String email = "";
    @Pattern(regexp="^$|\\+?[0-9 ()-]{7,24}") private String telefono = "";
    @Pattern(regexp="^$|[0-9]{8,15}") private String whatsapp = "";
    @Size(max=200) private String horario = "";
}
