package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.BenchmarkCpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BenchmarkCpuRepository extends JpaRepository<BenchmarkCpu, UUID> {
    List<BenchmarkCpu> findByCpuIdOrderByAplicacionAscPruebaAsc(UUID cpuId);
    List<BenchmarkCpu> findByCpuIdInOrderByAplicacionAscPruebaAsc(List<UUID> cpuIds);
}
