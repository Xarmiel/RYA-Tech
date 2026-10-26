package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    Optional<Pedido> findByNumeroPedido(String numeroPedido);
    Optional<Pedido> findByPreferenceId(String preferenceId);
    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);
}

