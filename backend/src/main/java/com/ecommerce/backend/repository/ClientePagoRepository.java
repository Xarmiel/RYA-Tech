package com.ecommerce.backend.repository;
import com.ecommerce.backend.entity.ClientePago;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ClientePagoRepository extends JpaRepository<ClientePago,UUID> {
 Optional<ClientePago> findByUsuarioIdAndEntorno(UUID user,String scope);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ClientePago c where c.id=:id")
 Optional<ClientePago> lock(@Param("id") UUID id);
}
