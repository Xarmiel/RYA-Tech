package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.id=:id")
    Optional<Pedido> findForPaymentUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<Pedido> findByNumeroPedido(String numeroPedido);
    Optional<Pedido> findByPreferenceId(String preferenceId);
    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);
}

