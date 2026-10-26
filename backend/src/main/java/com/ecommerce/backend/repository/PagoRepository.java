package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {
    List<Pago> findByPedidoIdOrderByCreatedAtDesc(UUID pedidoId);
    Optional<Pago> findByMpPaymentId(String mpPaymentId);
}

