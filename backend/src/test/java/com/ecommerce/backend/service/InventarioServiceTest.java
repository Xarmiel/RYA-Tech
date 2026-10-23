package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AjusteStockRequestDto;
import com.ecommerce.backend.dto.AlmacenDto;
import com.ecommerce.backend.dto.InventarioItemDto;
import com.ecommerce.backend.dto.MovimientoInventarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.entity.Almacen;
import com.ecommerce.backend.entity.Existencia;
import com.ecommerce.backend.entity.MovimientoInventario;
import com.ecommerce.backend.entity.Producto;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.AlmacenRepository;
import com.ecommerce.backend.repository.ExistenciaRepository;
import com.ecommerce.backend.repository.ImagenProductoRepository;
import com.ecommerce.backend.repository.MovimientoInventarioRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.InventarioProductLockRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.ecommerce.backend.service.impl.InventarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventarioServiceTest {
    @org.junit.jupiter.api.AfterEach
    void clearSecurity(){ SecurityContextHolder.clearContext(); }

    private ExistenciaRepository existenciaRepository;
    private AlmacenRepository almacenRepository;
    private ProductoRepository productoRepository;
    private MovimientoInventarioRepository movimientoInventarioRepository;
    private ImagenProductoRepository imagenProductoRepository;
    private InventarioService inventarioService;
    private InventarioProductLockRepository productLock;

    private Producto producto;
    private Almacen almacen;
    private Existencia existencia;

    @BeforeEach
    void setUp() {
        existenciaRepository = mock(ExistenciaRepository.class);
        almacenRepository = mock(AlmacenRepository.class);
        productoRepository = mock(ProductoRepository.class);
        movimientoInventarioRepository = mock(MovimientoInventarioRepository.class);
        imagenProductoRepository = mock(ImagenProductoRepository.class);

        productLock = mock(InventarioProductLockRepository.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "RS256").subject("10000000-1000-4000-8000-000000000001").claim("app_metadata", java.util.Map.of("role", "admin")).build()));
        inventarioService = new InventarioServiceImpl(
                existenciaRepository,
                almacenRepository,
                productoRepository,
                movimientoInventarioRepository,
                imagenProductoRepository, productLock
        );

        UUID prodId = UUID.randomUUID();
        producto = Producto.builder()
                .id(prodId)
                .nombre("Procesador AMD Ryzen 7 7800X3D")
                .sku("AMD-7800X3D")
                .isDeleted(false)
                .build();

        UUID almId = UUID.randomUUID();
        almacen = Almacen.builder()
                .id(almId)
                .nombre("Almacén Principal")
                .codigo("ALM-01")
                .activo(true)
                .isDeleted(false)
                .build();

        existencia = Existencia.builder()
                .id(UUID.randomUUID())
                .producto(producto)
                .almacenId(almId)
                .cantidad(10)
                .reservado(2)
                .version(0)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("listarInventario devuelve existencias paginadas con cálculo real de stock")
    void testListarInventario() {
        Page<Existencia> page = new PageImpl<>(List.of(existencia));
        when(existenciaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(imagenProductoRepository.findByProductoIdInAndIsDeletedFalseOrderByPosicionAsc(anyList()))
                .thenReturn(Collections.emptyList());

        PaginatedResponseDto<InventarioItemDto> result = inventarioService.listarInventario(null, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        InventarioItemDto item = result.getContent().get(0);
        assertThat(item.getProductoNombre()).isEqualTo("Procesador AMD Ryzen 7 7800X3D");
        assertThat(item.getCantidad()).isEqualTo(10);
        assertThat(item.getReservado()).isEqualTo(2);
        assertThat(item.getDisponible()).isEqualTo(8);
        assertThat(item.getEstadoAlerta()).isEqualTo("DISPONIBLE");
    }

    @Test
    @DisplayName("listarAlmacenesActivos devuelve lista de almacenes disponibles")
    void testListarAlmacenes() {
        when(almacenRepository.findByActivoTrueAndIsDeletedFalseOrderByNombreAsc()).thenReturn(List.of(almacen));

        List<AlmacenDto> list = inventarioService.listarAlmacenesActivos();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCodigo()).isEqualTo("ALM-01");
        assertThat(list.get(0).getNombre()).isEqualTo("Almacén Principal");
    }

    @Test
    @DisplayName("registrarAjuste ENTRADA incrementa la cantidad y registra movimiento en auditoría")
    void testRegistrarEntrada() {
        when(productLock.bloquear(producto.getId())).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        when(existenciaRepository.buscarExacta(producto.getId(), almacen.getId(), null)).thenReturn(List.of(existencia));
        when(existenciaRepository.saveAndFlush(any(Existencia.class))).thenAnswer(i -> i.getArgument(0));

        AjusteStockRequestDto dto = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipo("ENTRADA")
                .cantidad(5)
                .nota("Recepción de nuevo lote del proveedor")
                .build();

        InventarioItemDto res = inventarioService.registrarAjuste(dto);

        assertThat(res.getCantidad()).isEqualTo(15);
        assertThat(res.getReservado()).isEqualTo(2);
        assertThat(res.getDisponible()).isEqualTo(13);

        verify(movimientoInventarioRepository, times(1)).save(argThat(mov ->
                mov.getTipo().equals("ENTRADA") &&
                mov.getCantidad() == 5 &&
                mov.getNota().contains("Recepción de nuevo lote")
        ));
    }

    @Test
    @DisplayName("registrarAjuste SALIDA descuenta la cantidad y registra movimiento")
    void testRegistrarSalida() {
        when(productLock.bloquear(producto.getId())).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        when(existenciaRepository.buscarExacta(producto.getId(), almacen.getId(), null)).thenReturn(List.of(existencia));
        when(existenciaRepository.saveAndFlush(any(Existencia.class))).thenAnswer(i -> i.getArgument(0));

        AjusteStockRequestDto dto = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipo("SALIDA")
                .cantidad(3)
                .nota("Merma por empaque dañado")
                .build();

        InventarioItemDto res = inventarioService.registrarAjuste(dto);

        assertThat(res.getCantidad()).isEqualTo(7);
        assertThat(res.getReservado()).isEqualTo(2);
        assertThat(res.getDisponible()).isEqualTo(5);

        verify(movimientoInventarioRepository, times(1)).save(argThat(mov ->
                mov.getTipo().equals("SALIDA") &&
                mov.getCantidad() == 3 &&
                mov.getNota().contains("Merma")
        ));
    }

    @Test
    @DisplayName("registrarAjuste SALIDA falla si se intenta retirar más de lo permitido reduciendo bajo reservas")
    void testRegistrarSalidaExcedeReservas() {
        when(productLock.bloquear(producto.getId())).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        when(existenciaRepository.buscarExacta(producto.getId(), almacen.getId(), null)).thenReturn(List.of(existencia));

        // Actual 10, reservado 2, disponible 8. Si pide retirar 9, quedaría 1 (< 2 reservado).
        AjusteStockRequestDto dto = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipo("SALIDA")
                .cantidad(9)
                .nota("Retiro excesivo")
                .build();

        assertThatThrownBy(() -> inventarioService.registrarAjuste(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No es posible retirar 9 unidades");

        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarAjuste AJUSTE actualiza la existencia física y registra el delta")
    void testRegistrarAjusteFisico() {
        when(productLock.bloquear(producto.getId())).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        when(existenciaRepository.buscarExacta(producto.getId(), almacen.getId(), null)).thenReturn(List.of(existencia));
        when(existenciaRepository.saveAndFlush(any(Existencia.class))).thenAnswer(i -> i.getArgument(0));

        AjusteStockRequestDto dto = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipo("AJUSTE")
                .cantidad(12)
                .nota("Conteo físico semestral")
                .build();

        InventarioItemDto res = inventarioService.registrarAjuste(dto);

        assertThat(res.getCantidad()).isEqualTo(12);
        assertThat(res.getReservado()).isEqualTo(2);
        assertThat(res.getDisponible()).isEqualTo(10);

        verify(movimientoInventarioRepository, times(1)).save(argThat(mov ->
                mov.getTipo().equals("AJUSTE") &&
                mov.getCantidad() == 2 &&
                mov.getNota().contains("Conteo físico")
        ));
    }

    @Test
    @DisplayName("registrarAjuste rechaza nota vacía para garantizar trazabilidad estricta")
    void testRechazaNotaVacia() {
        AjusteStockRequestDto dto = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipo("ENTRADA")
                .cantidad(5)
                .nota("   ")
                .build();

        assertThatThrownBy(() -> inventarioService.registrarAjuste(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("El motivo o justificación del ajuste es obligatorio");
    }

    @Test
    @DisplayName("obtenerKardex devuelve la lista de movimientos registrados")
    void testObtenerKardex() {
        when(existenciaRepository.existsById(existencia.getId())).thenReturn(true);
        MovimientoInventario mov = MovimientoInventario.builder()
                .id(UUID.randomUUID())
                .existencia(existencia)
                .tipo("ENTRADA")
                .cantidad(5)
                .nota("Lote inicial")
                .registradoAt(OffsetDateTime.now())
                .build();
        when(movimientoInventarioRepository.findByExistenciaId(eq(existencia.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mov)));

        List<MovimientoInventarioDto> kardex = inventarioService.obtenerKardex(existencia.getId());

        assertThat(kardex).hasSize(1);
        assertThat(kardex.get(0).getTipo()).isEqualTo("ENTRADA");
        assertThat(kardex.get(0).getCantidad()).isEqualTo(5);
    }
}
