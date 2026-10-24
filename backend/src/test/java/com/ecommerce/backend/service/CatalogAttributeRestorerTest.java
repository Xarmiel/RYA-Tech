package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.ValorAtributoProducto;
import com.ecommerce.backend.exception.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CatalogAttributeRestorerTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final CatalogAttributeRestorer restorer=new CatalogAttributeRestorer(jdbc);
    private final UUID actor=UUID.randomUUID();
    private final ValorAtributoProducto row=ValorAtributoProducto.builder().id(UUID.randomUUID()).isDeleted(true).build();
    private void auth(String claim,String subject){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg","RS256").subject(subject).claim(claim,Map.of("role","admin")).build()));}
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void unValorVigenteNoNecesitaRestauracion(){row.setIsDeleted(false);restorer.restore(row);verifyNoInteractions(jdbc);}
    @Test void rechazaSinSesionYNoConfiaEnUserMetadata(){assertThrows(ConflictException.class,()->restorer.restore(row));auth("user_metadata",actor.toString());assertThrows(ConflictException.class,()->restorer.restore(row));verifyNoInteractions(jdbc);}
    @Test void rechazaIdentificadorNoUuid(){auth("app_metadata","not-a-uuid");assertThrows(ConflictException.class,()->restorer.restore(row));verifyNoInteractions(jdbc);}
    @Test void faltaDeMigracionDaConflictoClaroAntesDeIntentarReactivar(){
        auth("app_metadata",actor.toString());when(jdbc.queryForObject(contains("to_regprocedure"),eq(Boolean.class))).thenReturn(false);
        var error=assertThrows(ConflictException.class,()->restorer.restore(row));assertTrue(error.getMessage().contains("Falta habilitar"));assertTrue(row.getIsDeleted());
        verify(jdbc,never()).queryForObject(eq("select rya.restaurar_atributo_catalogo(?, ?, ?)"),eq(Boolean.class),any(),any(),any());
    }
    @Test void usaProcesoAuditadoConActorConfiableSinCambiarBanderaDirectamente(){
        auth("app_metadata",actor.toString());when(jdbc.queryForObject(contains("to_regprocedure"),eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(eq("select rya.restaurar_atributo_catalogo(?, ?, ?)"),eq(Boolean.class),eq(row.getId()),eq(actor),anyString())).thenReturn(true);
        restorer.restore(row);assertTrue(row.getIsDeleted());verify(jdbc).queryForObject(eq("select rya.restaurar_atributo_catalogo(?, ?, ?)"),eq(Boolean.class),eq(row.getId()),eq(actor),contains("Restauración"));
    }
    @Test void rechazoDeBdSeDevuelveComoConflictoSinExponerSql(){
        auth("app_metadata",actor.toString());when(jdbc.queryForObject(contains("to_regprocedure"),eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForObject(eq("select rya.restaurar_atributo_catalogo(?, ?, ?)"),eq(Boolean.class),any(),any(),any())).thenThrow(new org.springframework.dao.PermissionDeniedDataAccessException("Detalle SQL reservado",null));
        var error=assertThrows(ConflictException.class,()->restorer.restore(row));assertTrue(error.getMessage().contains("sin cambios"));assertFalse(error.getMessage().contains("Detalle SQL reservado"));
    }
}
