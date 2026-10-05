package com.ecommerce.backend.repository;

import com.ecommerce.backend.model.Rol;
import com.ecommerce.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByRol(Rol rol);
    List<Usuario> findByRol_Nombre(String rolNombre);
}