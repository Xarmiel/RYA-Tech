package com.ecommerce.backend.repository;
import com.ecommerce.backend.entity.CampoFichaCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CampoFichaCategoriaRepository extends JpaRepository<CampoFichaCategoria,UUID> {
 List<CampoFichaCategoria> findByCategoriaIdOrderByCreatedAtAsc(UUID categoriaId);
}
