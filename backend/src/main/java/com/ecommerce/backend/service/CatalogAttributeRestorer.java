package com.ecommerce.backend.service;

import com.ecommerce.backend.config.AdminPermissions;
import com.ecommerce.backend.entity.ValorAtributoProducto;
import com.ecommerce.backend.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Joins the product transaction; it never disables triggers or creates duplicate attribute rows. */
@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CatalogAttributeRestorer {
    private final JdbcTemplate jdbc;

    @Transactional(propagation=Propagation.MANDATORY)
    public void restore(ValorAtributoProducto row) {
        if(!Boolean.TRUE.equals(row.getIsDeleted()))return;
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof JwtAuthenticationToken token)||!AdminPermissions.allowed(token.getToken()))
            throw new ConflictException("Restaurar un campo técnico requiere una sesión de administrador vigente.");
        UUID actor;
        try{actor=UUID.fromString(token.getToken().getSubject());}
        catch(IllegalArgumentException|NullPointerException ex){throw new ConflictException("La sesión administrativa no tiene un identificador válido.");}
        if(!Boolean.TRUE.equals(jdbc.queryForObject("select to_regprocedure('rya.restaurar_atributo_catalogo(uuid,uuid,text)') is not null",Boolean.class)))
            throw new ConflictException("Este campo técnico fue eliminado. Falta habilitar la restauración auditada en la BD. No borres el producto ni repitas la importación; el guardado no se aplicó.");
        Boolean restored;
        try{
            restored=jdbc.queryForObject("select rya.restaurar_atributo_catalogo(?, ?, ?)",Boolean.class,
                    row.getId(),actor,"Restauración de campo comercial al guardar su ficha técnica");
        }catch(DataAccessException ex){
            log.warn("La BD rechazó la restauración del atributo {}",row.getId(),ex);
            throw new ConflictException("La BD rechazó la restauración auditada del campo técnico. El guardado se canceló sin cambios; revisa los permisos y el registro del backend.");
        }
        if(!Boolean.TRUE.equals(restored))throw new ConflictException("No se pudo confirmar la restauración del campo técnico. El guardado no se aplicó.");
    }
}
