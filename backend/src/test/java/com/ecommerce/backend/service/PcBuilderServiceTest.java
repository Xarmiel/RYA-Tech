package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.mapper.ProductoMapper;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.impl.PcBuilderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PcBuilderServiceTest {

    @Mock
    private TipoComponenteRepository tipoComponenteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ImagenProductoRepository imagenProductoRepository;
    @Mock
    private ExistenciaRepository existenciaRepository;
    @Mock
    private EspecificacionCpuRepository especificacionCpuRepository;
    @Mock
    private RevisionPlacaRepository revisionPlacaRepository;
    @Mock
    private VersionBiosRepository versionBiosRepository;
    @Mock
    private CompatibilidadCpuPlacaRepository compatibilidadCpuPlacaRepository;
    @Mock
    private ValorAtributoProductoRepository valorAtributoProductoRepository;

    private final ProductoMapper productoMapper = new ProductoMapper();

    private PcBuilderServiceImpl pcBuilderService;

    private UUID cpuId;
    private UUID placaId;
    private UUID revisionId;
    private EspecificacionCpu cpu;
    private RevisionPlaca revision;

    @BeforeEach
    void setUp() {
        pcBuilderService = new PcBuilderServiceImpl(
                tipoComponenteRepository,
                productoRepository,
                categoriaRepository,
                imagenProductoRepository,
                existenciaRepository,
                especificacionCpuRepository,
                revisionPlacaRepository,
                versionBiosRepository,
                compatibilidadCpuPlacaRepository,
                valorAtributoProductoRepository,
                productoMapper
        );

        cpuId = UUID.randomUUID();
        placaId = UUID.randomUUID();
        revisionId = UUID.randomUUID();

        Producto prodCpu = Producto.builder().id(cpuId).nombre("AMD Ryzen 7 7800X3D").sku("CPU-AMD-7800X3D").build();
        cpu = EspecificacionCpu.builder()
                .productoId(cpuId)
                .producto(prodCpu)
                .modelo("Ryzen 7 7800X3D")
                .socket("AM5")
                .familia("Ryzen 7")
                .generacion("Zen 4")
                .nucleos(8)
                .hilos(16)
                .potenciaBaseW(new BigDecimal("120.00"))
                .potenciaMaximaW(new BigDecimal("162.00"))
                .tieneGraficosIntegrados(true)
                .build();

        revision = RevisionPlaca.builder()
                .id(revisionId)
                .productoId(placaId)
                .modelo("B650 AORUS ELITE AX")
                .revision("Rev 1.0")
                .socket("AM5")
                .chipset("B650")
                .build();
    }

    @Test
    @DisplayName("Debe validar compatibilidad VERIFICADA cuando el fabricante certifica soporte oficial y BIOS")
    void testValidarCambioSoportado() {
        when(especificacionCpuRepository.findConProductoPorId(cpuId)).thenReturn(Optional.of(cpu));
        when(revisionPlacaRepository.findByProductoId(placaId)).thenReturn(List.of(revision));

        VersionBios biosMinima = VersionBios.builder()
                .id(UUID.randomUUID())
                .placaRevisionId(revisionId)
                .version("F1")
                .orden(1)
                .build();

        CompatibilidadCpuPlaca compat = CompatibilidadCpuPlaca.builder()
                .id(new CompatibilidadCpuPlacaId(cpuId, revisionId))
                .soporte("SOPORTADO")
                .biosMinima(biosMinima)
                .build();

        when(compatibilidadCpuPlacaRepository.findConDetalles(cpuId, revisionId)).thenReturn(Optional.of(compat));
        when(versionBiosRepository.findByPlacaRevisionIdAndVersionIgnoreCase(revisionId, "F1"))
                .thenReturn(Optional.of(biosMinima));

        ValidarCambioRequestDto req = ValidarCambioRequestDto.builder()
                .cpuId(cpuId)
                .placaId(placaId)
                .biosInstalada("F1")
                .build();

        ValidarCambioResponseDto res = pcBuilderService.validarCambio(req);

        assertNotNull(res);
        assertTrue(res.getEsCompatible());
        assertEquals("VERIFICADA", res.getEstadoCompatibilidad());
        assertEquals("F1", res.getBiosMinimaRequerida());
    }

    @Test
    @DisplayName("Una selección incompleta debe quedar POR_VERIFICAR")
    void testValidarCambioIncompleto() {
        ValidarCambioResponseDto res = pcBuilderService.validarCambio(
                ValidarCambioRequestDto.builder().cpuId(cpuId).build()
        );

        assertFalse(res.getEsCompatible());
        assertEquals("POR_VERIFICAR", res.getEstadoCompatibilidad());
        verifyNoInteractions(especificacionCpuRepository, compatibilidadCpuPlacaRepository);
    }

    @Test
    @DisplayName("Soporte oficial sin BIOS instalada debe ser CONDICIONAL")
    void testValidarCambioSinBiosInstalada() {
        when(especificacionCpuRepository.findConProductoPorId(cpuId)).thenReturn(Optional.of(cpu));
        when(revisionPlacaRepository.findByProductoId(placaId)).thenReturn(List.of(revision));

        VersionBios biosMinima = VersionBios.builder()
                .id(UUID.randomUUID()).placaRevisionId(revisionId).version("F5").orden(5).build();
        CompatibilidadCpuPlaca compat = CompatibilidadCpuPlaca.builder()
                .id(new CompatibilidadCpuPlacaId(cpuId, revisionId))
                .soporte("SOPORTADO")
                .biosMinima(biosMinima)
                .build();
        when(compatibilidadCpuPlacaRepository.findConDetalles(cpuId, revisionId)).thenReturn(Optional.of(compat));

        ValidarCambioResponseDto res = pcBuilderService.validarCambio(
                ValidarCambioRequestDto.builder().cpuId(cpuId).placaId(placaId).build()
        );

        assertTrue(res.getEsCompatible());
        assertEquals("CONDICIONAL", res.getEstadoCompatibilidad());
        assertFalse(res.getRequiereActualizacionBios());
        assertEquals("F5", res.getBiosMinimaRequerida());
    }

    @Test
    @DisplayName("Debe retornar CONDICIONAL con actualizacion de BIOS si la BIOS instalada es menor a la requerida")
    void testValidarCambioBiosMenor() {
        when(especificacionCpuRepository.findConProductoPorId(cpuId)).thenReturn(Optional.of(cpu));
        when(revisionPlacaRepository.findByProductoId(placaId)).thenReturn(List.of(revision));

        VersionBios biosMinima = VersionBios.builder()
                .id(UUID.randomUUID())
                .placaRevisionId(revisionId)
                .version("F5")
                .orden(5)
                .build();

        CompatibilidadCpuPlaca compat = CompatibilidadCpuPlaca.builder()
                .id(new CompatibilidadCpuPlacaId(cpuId, revisionId))
                .soporte("SOPORTADO")
                .biosMinima(biosMinima)
                .build();

        when(compatibilidadCpuPlacaRepository.findConDetalles(cpuId, revisionId)).thenReturn(Optional.of(compat));

        VersionBios biosInstalada = VersionBios.builder()
                .id(UUID.randomUUID())
                .placaRevisionId(revisionId)
                .version("F2")
                .orden(2)
                .build();
        when(versionBiosRepository.findByPlacaRevisionIdAndVersionIgnoreCase(revisionId, "F2"))
                .thenReturn(Optional.of(biosInstalada));

        ValidarCambioRequestDto req = ValidarCambioRequestDto.builder()
                .cpuId(cpuId)
                .placaId(placaId)
                .biosInstalada("F2")
                .build();

        ValidarCambioResponseDto res = pcBuilderService.validarCambio(req);

        assertNotNull(res);
        assertTrue(res.getEsCompatible());
        assertEquals("CONDICIONAL", res.getEstadoCompatibilidad());
        assertTrue(res.getRequiereActualizacionBios());
        assertEquals("F5", res.getBiosMinimaRequerida());
    }

    @Test
    @DisplayName("No debe declarar compatibilidad únicamente por socket si falta evidencia en tabla de compatibilidad")
    void testValidarCambioSocketCoincidePeroSinEvidencia() {
        when(especificacionCpuRepository.findConProductoPorId(cpuId)).thenReturn(Optional.of(cpu));
        when(revisionPlacaRepository.findByProductoId(placaId)).thenReturn(List.of(revision));
        when(compatibilidadCpuPlacaRepository.findConDetalles(cpuId, revisionId)).thenReturn(Optional.empty());

        ValidarCambioRequestDto req = ValidarCambioRequestDto.builder()
                .cpuId(cpuId)
                .placaId(placaId)
                .build();

        ValidarCambioResponseDto res = pcBuilderService.validarCambio(req);

        assertNotNull(res);
        assertFalse(res.getEsCompatible());
        assertEquals("POR_VERIFICAR", res.getEstadoCompatibilidad());
        assertTrue(res.getMensajes().get(0).contains("No se declara compatibilidad únicamente por socket"));
    }

    @Test
    @DisplayName("Debe calcular total de configuración en BigDecimal y estimar potencia y fuente recomendada")
    void testCalcularConfiguracion() {
        Producto prod = Producto.builder()
                .id(cpuId)
                .nombre("AMD Ryzen 7 7800X3D")
                .sku("CPU-AMD-7800X3D")
                .precioBase(new BigDecimal("1650.00"))
                .activo(true)
                .vendible(true)
                .isDeleted(false)
                .build();

        when(productoRepository.findAllById(List.of(cpuId))).thenReturn(List.of(prod));
        List<Object[]> stockRows = Collections.singletonList(new Object[]{cpuId, 5L});
        when(existenciaRepository.findStockDisponiblePorProductoIds(List.of(cpuId)))
                .thenReturn(stockRows);
        when(especificacionCpuRepository.findById(cpuId)).thenReturn(Optional.of(cpu));

        CalcularConfiguracionRequestDto req = CalcularConfiguracionRequestDto.builder()
                .items(List.of(ItemConfiguracionDto.builder().productoId(cpuId).cantidad(1).build()))
                .build();

        CalcularConfiguracionResponseDto res = pcBuilderService.calcularConfiguracion(req);

        assertNotNull(res);
        assertEquals(new BigDecimal("1650.00"), res.getPrecioTotal());
        assertEquals(new BigDecimal("162.00"), res.getConsumoEstimadoW());
        assertNotNull(res.getFuenteRecomendadaW());
        assertTrue(res.getFuenteRecomendadaW() >= 450);
        assertTrue(res.getTodosDisponibles());
    }

    @Test
    @DisplayName("Debe rechazar RAM cuyo tipo no coincide con la placa madre")
    void testRamIncompatibleConPlaca() {
        prepararPlataformaVerificada();
        UUID ramId = UUID.randomUUID();
        Producto placa = Producto.builder().id(placaId).build();
        Producto ram = Producto.builder().id(ramId).build();
        when(valorAtributoProductoRepository.findConAtributoPorProductoIds(anyList())).thenReturn(List.of(
                atributoTexto(placa, "tipo_ram_placa", "DDR5"),
                atributoTexto(ram, "tipo_ram", "DDR4"),
                atributoNumero(placa, "capacidad_ram_max_placa_gb", "128"),
                atributoNumero(ram, "capacidad_ram_gb", "32"),
                atributoNumero(placa, "ranuras_ram_placa", "4"),
                atributoNumero(ram, "modulos_ram", "2"),
                atributoNumero(placa, "velocidad_ram_max_placa_mt_s", "6400"),
                atributoNumero(ram, "velocidad_ram_mt_s", "6000")
        ));

        ValidarCambioResponseDto resultado = pcBuilderService.validarCambio(ValidarCambioRequestDto.builder()
                .cpuId(cpuId).placaId(placaId).biosInstalada("F1").ramId(ramId).build());

        assertFalse(resultado.getEsCompatible());
        assertEquals("INCOMPATIBLE", resultado.getEstadoCompatibilidad());
        assertTrue(resultado.getMensajes().stream().anyMatch(m -> m.contains("DDR4") && m.contains("DDR5")));
    }

    @Test
    @DisplayName("Debe impedir retirar un componente bloqueado")
    void testRespetaComponenteBloqueado() {
        prepararPlataformaVerificada();
        UUID bloqueado = UUID.randomUUID();

        ValidarCambioResponseDto resultado = pcBuilderService.validarCambio(ValidarCambioRequestDto.builder()
                .cpuId(cpuId).placaId(placaId).biosInstalada("F1")
                .componentesBloqueados(List.of(bloqueado))
                .build());

        assertFalse(resultado.getEsCompatible());
        assertEquals("INCOMPATIBLE", resultado.getEstadoCompatibilidad());
        assertTrue(resultado.getMensajes().stream().anyMatch(m -> m.contains("bloqueados")));
    }

    private void prepararPlataformaVerificada() {
        when(especificacionCpuRepository.findConProductoPorId(cpuId)).thenReturn(Optional.of(cpu));
        when(revisionPlacaRepository.findByProductoId(placaId)).thenReturn(List.of(revision));
        VersionBios bios = VersionBios.builder()
                .id(UUID.randomUUID()).placaRevisionId(revisionId).version("F1").orden(1).build();
        CompatibilidadCpuPlaca compatibilidad = CompatibilidadCpuPlaca.builder()
                .id(new CompatibilidadCpuPlacaId(cpuId, revisionId))
                .soporte("SOPORTADO").biosMinima(bios).build();
        when(compatibilidadCpuPlacaRepository.findConDetalles(cpuId, revisionId)).thenReturn(Optional.of(compatibilidad));
        when(versionBiosRepository.findByPlacaRevisionIdAndVersionIgnoreCase(revisionId, "F1"))
                .thenReturn(Optional.of(bios));
    }

    private ValorAtributoProducto atributoTexto(Producto producto, String codigo, String valor) {
        return ValorAtributoProducto.builder()
                .producto(producto)
                .atributo(AtributoTecnico.builder().codigo(codigo).build())
                .valorTexto(valor)
                .build();
    }

    private ValorAtributoProducto atributoNumero(Producto producto, String codigo, String valor) {
        return ValorAtributoProducto.builder()
                .producto(producto)
                .atributo(AtributoTecnico.builder().codigo(codigo).build())
                .valorNumero(new BigDecimal(valor))
                .build();
    }
}
