package com.ecommerce.backend.service.implement;

import com.ecommerce.backend.dto.request.UsuarioCreateDto;
import com.ecommerce.backend.dto.request.UsuarioLoginDto;
import com.ecommerce.backend.dto.response.UsuarioResponseDto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.exception.UnauthorizedException;
import com.ecommerce.backend.mapper.UsuarioMapper;
import com.ecommerce.backend.model.Rol;
import com.ecommerce.backend.model.Usuario;
import com.ecommerce.backend.repository.RolRepository;
import com.ecommerce.backend.repository.UsuarioRepository;
import com.ecommerce.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponseDto crearUsuario(UsuarioCreateDto dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BadRequestException("El email ya se encuentra registrado: " + dto.email());
        }

        String rolNombre = (dto.rol() != null && !dto.rol().isBlank())
                ? dto.rol().trim().toUpperCase()
                : "CLIENT";

        if ("CLIENTE".equals(rolNombre)) {
            rolNombre = "CLIENT";
        }

        String finalRolNombre = rolNombre;
        Rol rol = rolRepository.findByNombre(finalRolNombre)
                .orElseThrow(() -> new BadRequestException("El rol '" + finalRolNombre + "' no existe. Roles válidos: CLIENT, ADMIN"));

        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setRol(rol);

        return usuarioMapper.toResponseDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto autenticar(UsuarioLoginDto dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
            .filter(candidato -> passwordEncoder.matches(dto.password(), candidato.getPasswordHash()))
            .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos."));
        return usuarioMapper.toResponseDto(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorId(UUID id) {
        return usuarioRepository.findById(id)
            .map(usuarioMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
            .map(usuarioMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarTodos() {
        return usuarioRepository.findAll().stream()
            .map(usuarioMapper::toResponseDto)
            .toList();
    }
}