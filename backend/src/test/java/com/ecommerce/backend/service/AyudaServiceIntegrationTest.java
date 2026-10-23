package com.ecommerce.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AyudaServiceIntegrationTest {
    @Autowired AyudaService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void soloExponePreguntasPublicadasYCategoriasNoEliminadas() {
        UUID category = categoria("Pedidos", false), archived = categoria("Archivada", true);
        pregunta(category,"Pregunta visible","Respuesta pública",1,true,false);
        pregunta(category,"Borrador","No publicar",2,false,false);
        pregunta(category,"Eliminada","No mostrar",3,true,true);
        pregunta(archived,"Categoría eliminada","No mostrar",1,true,false);
        var response = service.preguntas(null,null,0,12);
        assertEquals(1,response.totalElements());
        assertEquals("Pregunta visible",response.content().getFirst().pregunta());
        assertEquals(1,response.categorias().size());
    }

    @Test
    void buscaEnPreguntaYRespuestaIgnorandoMayusculasYTildes() {
        pregunta(null,"¿Cómo consulto mi envío?","Revisa la sección de compras.",1,true,false);
        assertEquals(1,service.preguntas("ENVIO",null,0,12).totalElements());
        assertEquals(1,service.preguntas("seccion",null,0,12).totalElements());
        assertEquals("General",service.preguntas(null,null,0,12).content().getFirst().categoria());
    }

    @Test
    void filtraAntesDePaginarYRespetaPosicion() {
        UUID category = categoria("Mi cuenta",false), other = categoria("Compras",false);
        pregunta(category,"Segunda","Respuesta",2,true,false);
        pregunta(category,"Primera","Respuesta",1,true,false);
        pregunta(other,"Otra","Respuesta",0,true,false);
        var response = service.preguntas(null,category,1,1);
        assertEquals(2,response.totalElements());
        assertEquals(2,response.totalPages());
        assertEquals("Segunda",response.content().getFirst().pregunta());
        assertEquals(2,response.categorias().size());
    }

    @Test
    void noTrataPorcentajesComoComodinesYEsPublicoSinSesion() throws Exception {
        pregunta(null,"Descuento del 10%","Respuesta",0,true,false);
        pregunta(null,"Otra consulta","Respuesta",0,true,false);
        assertEquals(1,service.preguntas("%",null,0,12).totalElements());
        mvc.perform(get("/api/v1/ayuda/preguntas").param("q","%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/ayuda/preguntas").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ayuda/contacto")).andExpect(status().isOk());
    }

    private UUID categoria(String nombre,boolean deleted) {
        UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO rya.categorias_soporte (id,nombre,is_deleted) VALUES (?,?,?)",id,nombre,deleted); return id;
    }
    private void pregunta(UUID category,String q,String answer,int position,boolean published,boolean deleted) {
        jdbc.update("INSERT INTO rya.preguntas_frecuentes (id,categoria_id,pregunta,respuesta,posicion,publicada,is_deleted) VALUES (?,?,?,?,?,?,?)",
                UUID.randomUUID(),category,q,answer,position,published,deleted);
    }
}
