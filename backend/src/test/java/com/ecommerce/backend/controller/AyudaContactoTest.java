package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.SoporteProperties;
import com.ecommerce.backend.service.AyudaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AyudaContactoTest {
    @Test
    void telefonoProvisionalNoGeneraEnlaceDeLlamada() {
        var config = new SoporteProperties(); config.setTelefono("000 000 000");
        var result = new AyudaController(mock(AyudaService.class),config).contacto();
        assertEquals("000 000 000",result.telefono()); assertNull(result.telefonoUrl());
    }
    @Test
    void noInventaCanalesCuandoNoEstanConfigurados() {
        var result = new AyudaController(mock(AyudaService.class),new SoporteProperties()).contacto();
        assertNull(result.email()); assertNull(result.telefonoUrl()); assertNull(result.whatsappUrl()); assertNull(result.horario());
    }
    @Test
    void construyeEnlacesDeContactoDesdeLaConfiguracion() {
        var config = new SoporteProperties();
        config.setEmail("contacto@example.test"); config.setTelefono("+51 999 123 456");
        config.setWhatsapp("51999123456"); config.setHorario("Horario de prueba");
        var result = new AyudaController(mock(AyudaService.class),config).contacto();
        assertEquals("tel:+51999123456",result.telefonoUrl());
        assertEquals("https://wa.me/51999123456",result.whatsappUrl());
        assertEquals(config.getEmail(),result.email()); assertEquals(config.getHorario(),result.horario());
    }
}
