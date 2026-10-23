package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.ActualizarPerfilRequestDto;
import com.ecommerce.backend.dto.PerfilUsuarioDto;
import com.ecommerce.backend.entity.Usuario;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.repository.UsuarioRepository;
import com.ecommerce.backend.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {
    private final UsuarioRepository repository;

    @Override
    @Transactional
    public PerfilUsuarioDto obtenerOSincronizar(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        Usuario usuario = repository.findById(id).orElseGet(() -> repository.save(nuevoUsuario(jwt, id)));
        validarEstado(usuario);
        sincronizarEmail(usuario, jwt.getClaimAsString("email"));
        return mapear(usuario, jwt);
    }

    @Override
    @Transactional
    public PerfilUsuarioDto actualizar(Jwt jwt, ActualizarPerfilRequestDto request) {
        UUID id = UUID.fromString(jwt.getSubject());
        Usuario usuario = repository.findByIdAndIsDeletedFalse(id)
                .orElseGet(() -> repository.save(nuevoUsuario(jwt, id)));
        validarEstado(usuario);
        usuario.setNombre(request.getNombre().trim());
        usuario.setTelefono(limpiar(request.getTelefono()));
        sincronizarEmail(usuario, jwt.getClaimAsString("email"));
        return mapear(repository.save(usuario), jwt);
    }

    private Usuario nuevoUsuario(Jwt jwt, UUID id) {
        Map<String, Object> metadata = jwt.getClaim("user_metadata");
        String nombre = metadata == null ? null : limpiar(valor(metadata.get("nombre")));
        String telefono = metadata == null ? null : limpiar(valor(metadata.get("telefono")));
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) throw new BadRequestException("El token no contiene un correo válido");
        if (nombre == null) nombre = email.substring(0, email.indexOf('@'));
        return Usuario.builder().id(id).nombre(nombre).email(email).telefono(telefono)
                .estado("ACTIVO").isDeleted(false).build();
    }

    private void sincronizarEmail(Usuario usuario, String email) {
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(usuario.getEmail())) {
            usuario.setEmail(email);
            repository.save(usuario);
        }
    }

    private void validarEstado(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getIsDeleted()) || !"ACTIVO".equals(usuario.getEstado())) {
            throw new BadRequestException("La cuenta no está activa");
        }
    }

    private PerfilUsuarioDto mapear(Usuario usuario, Jwt jwt) {
        Map<String, Object> metadata = jwt.getClaim("user_metadata");
        boolean emailVerificado = Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                || metadata != null && Boolean.TRUE.equals(metadata.get("email_verified"));
        return PerfilUsuarioDto.builder().id(usuario.getId()).nombre(usuario.getNombre())
                .email(usuario.getEmail()).telefono(usuario.getTelefono()).estado(usuario.getEstado())
                .emailVerificado(emailVerificado).build();
    }

    private String valor(Object value) { return value == null ? null : value.toString(); }
    private String limpiar(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
