package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CpuComparacionRequestDto;
import com.ecommerce.backend.dto.CpuComparacionResponseDto;
import com.ecommerce.backend.dto.CpuDetalleDto;
import com.ecommerce.backend.dto.CpuResumenDto;
import com.ecommerce.backend.entity.BenchmarkCpu;
import com.ecommerce.backend.entity.EspecificacionCpu;
import com.ecommerce.backend.entity.Producto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.mapper.CpuMapper;
import com.ecommerce.backend.mapper.ProductoMapper;
import com.ecommerce.backend.repository.BenchmarkCpuRepository;
import com.ecommerce.backend.repository.EspecificacionCpuRepository;
import com.ecommerce.backend.repository.ExistenciaRepository;
import com.ecommerce.backend.repository.ImagenProductoRepository;
import com.ecommerce.backend.service.impl.CpuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpuServiceTest {

    @Mock
    private EspecificacionCpuRepository especificacionCpuRepository;
    @Mock
    private BenchmarkCpuRepository benchmarkCpuRepository;
    @Mock
    private ImagenProductoRepository imagenProductoRepository;
    @Mock
    private ExistenciaRepository existenciaRepository;

    private final ProductoMapper productoMapper = new ProductoMapper();
    private final CpuMapper cpuMapper = new CpuMapper(productoMapper);

    private CpuServiceImpl cpuService;

    private UUID cpu1Id;
    private UUID cpu2Id;
    private EspecificacionCpu cpu1;
    private EspecificacionCpu cpu2;

    @BeforeEach
    void setUp() {
        cpuService = new CpuServiceImpl(
                especificacionCpuRepository,
                benchmarkCpuRepository,
                imagenProductoRepository,
                existenciaRepository,
                cpuMapper
        );

        cpu1Id = UUID.randomUUID();
        cpu2Id = UUID.randomUUID();

        Producto p1 = Producto.builder().id(cpu1Id).nombre("AMD Ryzen 5 7600X").precioBase(new BigDecimal("950.00")).build();
        cpu1 = EspecificacionCpu.builder()
                .productoId(cpu1Id)
                .producto(p1)
                .modelo("Ryzen 5 7600X")
                .socket("AM5")
                .familia("Ryzen 5")
                .generacion("Zen 4")
                .nucleos(6)
                .hilos(12)
                .potenciaBaseW(new BigDecimal("105.00"))
                .tieneGraficosIntegrados(true)
                .build();

        Producto p2 = Producto.builder().id(cpu2Id).nombre("AMD Ryzen 7 7800X3D").precioBase(new BigDecimal("1650.00")).build();
        cpu2 = EspecificacionCpu.builder()
                .productoId(cpu2Id)
                .producto(p2)
                .modelo("Ryzen 7 7800X3D")
                .socket("AM5")
                .familia("Ryzen 7")
                .generacion("Zen 4")
                .nucleos(8)
                .hilos(16)
                .potenciaBaseW(new BigDecimal("120.00"))
                .tieneGraficosIntegrados(true)
                .build();
    }

    @Test
    @DisplayName("Debe listar procesadores activos y calcular stock disponible")
    void testListarCpus() {
        when(especificacionCpuRepository.findAllActivos()).thenReturn(List.of(cpu1, cpu2));
        when(existenciaRepository.findStockDisponiblePorProductoId(cpu1Id)).thenReturn(10);
        when(existenciaRepository.findStockDisponiblePorProductoId(cpu2Id)).thenReturn(5);

        List<CpuResumenDto> list = cpuService.listarCpus(null, "AM5", null, null, null);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("Ryzen 5 7600X", list.get(0).getModelo());
        assertEquals("Ryzen 7 7800X3D", list.get(1).getModelo());
    }

    @Test
    @DisplayName("Debe comparar hasta 3 procesadores y calcular diferencias de precio respecto a la referencia")
    void testCompararCpus() {
        when(especificacionCpuRepository.findConProductoPorId(cpu1Id)).thenReturn(Optional.of(cpu1));
        when(especificacionCpuRepository.findConProductoPorId(cpu2Id)).thenReturn(Optional.of(cpu2));

        BenchmarkCpu bench = BenchmarkCpu.builder()
                .id(UUID.randomUUID())
                .cpuId(cpu1Id)
                .aplicacion("Cinebench R23")
                .versionAplicacion("R23")
                .prueba("Multi-Core")
                .metrica("Puntos")
                .unidad("pts")
                .valor(new BigDecimal("15200.00"))
                .mayorEsMejor(true)
                .medidoEl(LocalDate.now())
                .build();

        when(benchmarkCpuRepository.findByCpuIdInOrderByAplicacionAscPruebaAsc(List.of(cpu1Id, cpu2Id)))
                .thenReturn(List.of(bench));

        CpuComparacionRequestDto req = CpuComparacionRequestDto.builder()
                .cpuIds(List.of(cpu1Id, cpu2Id))
                .build();

        CpuComparacionResponseDto res = cpuService.compararCpus(req);

        assertNotNull(res);
        assertEquals(2, res.getProcesadores().size());
        BigDecimal difCpu2 = res.getDiferenciaPrecioReferencia().get(cpu2Id);
        assertEquals(0, difCpu2.compareTo(new BigDecimal("700.00")));
        assertEquals(0, res.getDiferenciaPrecioReferencia().get(cpu1Id).compareTo(BigDecimal.ZERO));
        assertEquals(1, res.getBenchmarks().size());
    }

    @Test
    @DisplayName("Debe rechazar la comparación si se envían más de 3 procesadores")
    void testCompararMasDeTresRechazado() {
        CpuComparacionRequestDto req = CpuComparacionRequestDto.builder()
                .cpuIds(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .build();

        assertThrows(BadRequestException.class, () -> cpuService.compararCpus(req));
    }
}
