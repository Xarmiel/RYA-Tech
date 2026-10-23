package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AdminProductoDetalleDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoDetalleDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.mapper.ProductoMapper;
import com.ecommerce.backend.repository.AtributoTecnicoRepository;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.repository.ExistenciaRepository;
import com.ecommerce.backend.repository.ImagenProductoRepository;
import com.ecommerce.backend.repository.MarcaRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.TipoProductoRepository;
import com.ecommerce.backend.repository.ValorAtributoProductoRepository;
import com.ecommerce.backend.service.impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ImagenProductoRepository imagenProductoRepository;
    @Mock
    private ValorAtributoProductoRepository valorAtributoProductoRepository;
    @Mock
    private ExistenciaRepository existenciaRepository;
    @Mock
    private AtributoTecnicoRepository atributoTecnicoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MarcaRepository marcaRepository;
    @Mock
    private TipoProductoRepository tipoProductoRepository;

    private final ProductoMapper productoMapper = new ProductoMapper();

    private ProductoServiceImpl productoService;

    private Producto producto;
    private UUID productoId;

    @BeforeEach
    void setUp() {
        productoService = new ProductoServiceImpl(
                productoRepository,
                imagenProductoRepository,
                valorAtributoProductoRepository,
                existenciaRepository,
                atributoTecnicoRepository,
                categoriaRepository,
                marcaRepository,
                tipoProductoRepository,
                productoMapper
        );

        productoId = UUID.randomUUID();
        Categoria cat = Categoria.builder().id(UUID.randomUUID()).nombre("Procesadores").slug("procesadores").build();
        Marca marca = Marca.builder().id(UUID.randomUUID()).nombre("AMD").slug("amd").build();
        TipoProducto tipo = TipoProducto.builder().id(UUID.randomUUID()).codigo("HARDWARE").build();

        producto = Producto.builder()
                .id(productoId)
                .sku("CPU-AMD-7600X")
                .nombre("AMD Ryzen 5 7600X")
                .slug("amd-ryzen-5-7600x")
                .descripcion("Procesador potente de 6 núcleos")
                .precioBase(new BigDecimal("950.00"))
                .activo(true)
                .vendible(true)
                .isDeleted(false)
                .categoria(cat)
                .marca(marca)
                .tipo(tipo)
                .build();
    }

    @Test
    @DisplayName("Debe listar productos activos paginados evitando N+1 en stock e imágenes")
    void testListarProductos() {
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto)));

        ImagenProducto img = ImagenProducto.builder()
                .id(UUID.randomUUID())
                .producto(producto)
                .url("https://example.com/cpu.jpg")
                .principal(true)
                .build();
        when(imagenProductoRepository.findByProductoIdInAndIsDeletedFalseOrderByPosicionAsc(List.of(productoId)))
                .thenReturn(List.of(img));

        List<Object[]> stockRows = Collections.singletonList(new Object[]{productoId, 15L});
        when(existenciaRepository.findStockDisponiblePorProductoIds(List.of(productoId)))
                .thenReturn(stockRows);

        PaginatedResponseDto<ProductoResumenDto> res = productoService.listarProductos(
                null, null, null, null, null, null, null, false, Map.of(), 0, 10, "nombre", "asc"
        );

        assertNotNull(res);
        assertEquals(1, res.getContent().size());
        ProductoResumenDto item = res.getContent().get(0);
        assertEquals("AMD Ryzen 5 7600X", item.getNombre());
        assertEquals("https://example.com/cpu.jpg", item.getImagenPrincipal());
        assertEquals(15, item.getStockDisponible());
    }

    @Test
    @DisplayName("Debe obtener producto completo por ID")
    void testObtenerPorId() {
        when(productoRepository.findConDetallesPorId(productoId)).thenReturn(Optional.of(producto));
        when(imagenProductoRepository.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(productoId))
                .thenReturn(List.of());
        when(valorAtributoProductoRepository.findConAtributoPorProductoId(productoId))
                .thenReturn(List.of());
        when(existenciaRepository.findStockDisponiblePorProductoId(productoId))
                .thenReturn(20);

        ProductoDetalleDto detalle = productoService.obtenerPorId(productoId);

        assertNotNull(detalle);
        assertEquals("CPU-AMD-7600X", detalle.getSku());
        assertEquals(20, detalle.getStockDisponible());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el producto por ID no existe")
    void testObtenerPorIdNoExiste() {
        when(productoRepository.findConDetallesPorId(productoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerPorId(productoId));
    }

    @Test
    @DisplayName("Debe rechazar rangos de precio invertidos")
    void testRangoPrecioInvalido() {
        assertThrows(BadRequestException.class, () -> productoService.listarProductos(
                null, null, null, null, null,
                new BigDecimal("1000.00"), new BigDecimal("500.00"),
                false, Map.of(), 0, 12, "nombre", "asc"
        ));

        verifyNoInteractions(productoRepository);
    }

    @Test
    @DisplayName("Debe crear un producto correctamente con stock inicial y estado borrador")
    void testCrearProducto() {
        UUID catId = UUID.randomUUID();
        UUID marcaId = UUID.randomUUID();
        Categoria cat = Categoria.builder().id(catId).nombre("Memorias").slug("memorias").build();
        Marca m = Marca.builder().id(marcaId).nombre("Kingston").slug("kingston").build();

        when(productoRepository.existsBySkuAndIsDeletedFalse("RAM-32GB-DDR5")).thenReturn(false);
        when(categoriaRepository.findById(catId)).thenReturn(Optional.of(cat));
        when(marcaRepository.findById(marcaId)).thenReturn(Optional.of(m));
        when(tipoProductoRepository.findByCodigoAndIsDeletedFalse("HW")).thenReturn(Optional.of(TipoProducto.builder().id(UUID.randomUUID()).codigo("HW").build()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        com.ecommerce.backend.dto.AdminProductoDto dto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Memoria RAM Kingston Fury 32GB DDR5")
                .sku("RAM-32GB-DDR5")
                .categoriaId(catId)
                .marcaId(marcaId)
                .precioBase(new BigDecimal("520.00"))
                .costoReferencia(new BigDecimal("450.00"))
                .stockInicial(10)
                .estado("draft")
                .imagenPrincipalUrl("https://example.com/ram.jpg")
                .build();

        AdminProductoDetalleDto creado = productoService.crearProducto(dto);

        assertNotNull(creado);
        assertEquals("RAM-32GB-DDR5", creado.getSku());
        assertFalse(creado.getActivo());
        assertFalse(creado.getVendible());
        assertEquals(new BigDecimal("450.00"), creado.getCostoReferencia());
        verify(existenciaRepository, times(1)).save(any(Existencia.class));
        verify(imagenProductoRepository, times(1)).save(any(ImagenProducto.class));
    }

    @Test
    @DisplayName("Debe crear y actualizar producto guardando la galería completa con orden y principal")
    void testCrearYActualizarProductoConGaleriaMultiple() {
        UUID catId = UUID.randomUUID();
        UUID marcaId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        Categoria cat = Categoria.builder().id(catId).nombre("Chasis").slug("chasis").build();
        Marca m = Marca.builder().id(marcaId).nombre("NZXT").slug("nzxt").build();

        when(productoRepository.existsBySkuAndIsDeletedFalse("CASE-H9-FLOW")).thenReturn(false);
        when(categoriaRepository.findById(catId)).thenReturn(Optional.of(cat));
        when(marcaRepository.findById(marcaId)).thenReturn(Optional.of(m));
        when(tipoProductoRepository.findByCodigoAndIsDeletedFalse("HW")).thenReturn(Optional.of(TipoProducto.builder().id(UUID.randomUUID()).codigo("HW").build()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            if (p.getId() == null) p.setId(prodId);
            return p;
        });

        var img1 = com.ecommerce.backend.dto.ImagenProductoDto.builder()
                .url("https://example.com/h9-front.jpg")
                .textoAlternativo("Frente H9")
                .posicion(0)
                .principal(true)
                .build();
        var img2 = com.ecommerce.backend.dto.ImagenProductoDto.builder()
                .url("https://example.com/h9-side.jpg")
                .textoAlternativo("Lateral vidrio")
                .posicion(1)
                .principal(false)
                .build();

        com.ecommerce.backend.dto.AdminProductoDto createDto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("NZXT H9 Flow White")
                .sku("CASE-H9-FLOW")
                .categoriaId(catId)
                .marcaId(marcaId)
                .precioBase(new BigDecimal("680.00"))
                .imagenes(List.of(img1, img2))
                .build();

        AdminProductoDetalleDto creado = productoService.crearProducto(createDto);
        assertNotNull(creado);
        verify(imagenProductoRepository, times(2)).save(any(ImagenProducto.class));

        // Probar actualización de galería
        Producto existente = Producto.builder()
                .id(prodId)
                .nombre("NZXT H9 Flow White")
                .sku("CASE-H9-FLOW")
                .categoria(cat)
                .marca(m)
                .isDeleted(false)
                .build();
        when(productoRepository.findById(prodId)).thenReturn(Optional.of(existente));
        when(imagenProductoRepository.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(prodId))
                .thenReturn(List.of(
                        ImagenProducto.builder().id(UUID.randomUUID()).producto(existente).url("https://example.com/h9-front.jpg").posicion(0).principal(true).isDeleted(false).build(),
                        ImagenProducto.builder().id(UUID.randomUUID()).producto(existente).url("https://example.com/h9-side.jpg").posicion(1).principal(false).isDeleted(false).build()
                ));

        var img3 = com.ecommerce.backend.dto.ImagenProductoDto.builder()
                .url("https://example.com/h9-updated.jpg")
                .textoAlternativo("H9 Actualizado")
                .posicion(0)
                .principal(true)
                .build();

        com.ecommerce.backend.dto.AdminProductoDto updateDto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("NZXT H9 Flow White RGB")
                .sku("CASE-H9-FLOW")
                .categoriaId(catId)
                .marcaId(marcaId)
                .precioBase(new BigDecimal("720.00"))
                .imagenes(List.of(img3))
                .build();

        AdminProductoDetalleDto actualizado = productoService.actualizarProducto(prodId, updateDto);
        assertNotNull(actualizado);
        verify(imagenProductoRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe cambiar estado entre publicar, desactivar y borrador")
    void testCambiarEstado() {
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // Publicar
        AdminProductoDetalleDto pub = productoService.cambiarEstado(productoId, "publicar");
        assertTrue(pub.getActivo());
        assertTrue(pub.getVendible());

        // Desactivar
        AdminProductoDetalleDto des = productoService.cambiarEstado(productoId, "desactivar");
        assertFalse(des.getActivo());
        assertFalse(des.getVendible());

        // Borrador
        AdminProductoDetalleDto bor = productoService.cambiarEstado(productoId, "borrador");
        assertFalse(bor.getActivo());
        assertFalse(bor.getVendible());
    }

    @Test
    @DisplayName("Debe eliminar producto mediante borrado lógico")
    void testEliminarProducto() {
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.eliminarProducto(productoId);

        assertTrue(producto.getIsDeleted());
        assertFalse(producto.getActivo());
        assertFalse(producto.getVendible());
        verify(productoRepository, times(1)).save(producto);
    }

    @Test
    @DisplayName("No debe alterar stock en actualización comercial de producto")
    void testActualizarProducto_NoAlteraStock() {
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        com.ecommerce.backend.dto.AdminProductoDto updateDto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("AMD Ryzen 5 7600X Updated")
                .precioBase(new BigDecimal("990.00"))
                .stockInicial(999) // Intento de alterar stock
                .build();

        productoService.actualizarProducto(productoId, updateDto);

        verify(existenciaRepository, never()).save(any(Existencia.class));
    }

    @Test
    @DisplayName("Debe vaciar galería al pasar lista vacía en actualización")
    void testActualizarProducto_VaciarGaleria() {
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ImagenProducto img1 = ImagenProducto.builder().id(UUID.randomUUID()).url("https://res.cloudinary.com/demo/image/upload/sample.jpg").isDeleted(false).build();
        when(imagenProductoRepository.findByProductoIdAndIsDeletedFalseOrderByPosicionAsc(productoId))
                .thenReturn(List.of(img1));

        com.ecommerce.backend.dto.AdminProductoDto updateDto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("AMD Ryzen 5 7600X No Images")
                .imagenes(Collections.emptyList())
                .build();

        productoService.actualizarProducto(productoId, updateDto);

        assertTrue(img1.getIsDeleted());
        verify(imagenProductoRepository, times(1)).saveAll(anyList());
        verify(imagenProductoRepository, never()).save(argThat(img -> !Boolean.TRUE.equals(img.getIsDeleted())));
    }

    @Test
    @DisplayName("Debe rechazar galería con más de 8 imágenes")
    void testValidarGaleria_RechazaMasDeOcho() {
        List<com.ecommerce.backend.dto.ImagenProductoDto> nueveImagenes = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nueveImagenes.add(com.ecommerce.backend.dto.ImagenProductoDto.builder()
                    .url("https://example.com/img" + i + ".png")
                    .build());
        }

        com.ecommerce.backend.dto.AdminProductoDto dto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test")
                .sku("SKU-TEST")
                .precioBase(new BigDecimal("100.00"))
                .imagenes(nueveImagenes)
                .build();

        assertThrows(BadRequestException.class, () -> productoService.crearProducto(dto));
    }

    @Test
    @DisplayName("Debe rechazar imágenes no HTTPS o con URLs blob")
    void testValidarGaleria_RechazaNoHttps() {
        var dtoHttp = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test")
                .sku("SKU-TEST-1")
                .precioBase(new BigDecimal("100.00"))
                .imagenes(List.of(com.ecommerce.backend.dto.ImagenProductoDto.builder()
                        .url("http://example.com/insecure.png")
                        .build()))
                .build();

        assertThrows(BadRequestException.class, () -> productoService.crearProducto(dtoHttp));

        var dtoBlob = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test")
                .sku("SKU-TEST-2")
                .precioBase(new BigDecimal("100.00"))
                .imagenes(List.of(com.ecommerce.backend.dto.ImagenProductoDto.builder()
                        .url("blob:http://localhost:4200/abc-123")
                        .build()))
                .build();

        assertThrows(BadRequestException.class, () -> productoService.crearProducto(dtoBlob));
    }

    @Test
    @DisplayName("Debe rechazar imágenes duplicadas y múltiples principales")
    void testValidarGaleria_RechazaDuplicadosYMultiplesPrincipales() {
        var dtoDup = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test")
                .sku("SKU-TEST-3")
                .precioBase(new BigDecimal("100.00"))
                .imagenes(List.of(
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img1.png").build(),
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img1.png").build()
                ))
                .build();

        assertThrows(BadRequestException.class, () -> productoService.crearProducto(dtoDup));

        var dtoMultiPri = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test")
                .sku("SKU-TEST-4")
                .precioBase(new BigDecimal("100.00"))
                .imagenes(List.of(
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img1.png").principal(true).build(),
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img2.png").principal(true).build()
                ))
                .build();

        assertThrows(BadRequestException.class, () -> productoService.crearProducto(dtoMultiPri));
    }

    @Test
    @DisplayName("Debe asegurar que solo la imagen indicada como principal sea guardada como principal")
    void testGuardarImagenesGaleria_SoloUnaPrincipal() {
        UUID catId = UUID.randomUUID();
        when(categoriaRepository.findById(catId)).thenReturn(Optional.of(Categoria.builder().id(catId).nombre("Cat").slug("cat").build()));
        when(tipoProductoRepository.findByCodigoAndIsDeletedFalse("HW")).thenReturn(Optional.of(TipoProducto.builder().id(UUID.randomUUID()).codigo("HW").build()));
        when(productoRepository.existsBySkuAndIsDeletedFalse("SKU-TEST-UNI")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var dto = com.ecommerce.backend.dto.AdminProductoDto.builder()
                .nombre("Test Producto")
                .sku("SKU-TEST-UNI")
                .categoriaId(catId)
                .precioBase(new BigDecimal("100.00"))
                .imagenes(List.of(
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img1.png").principal(false).posicion(0).build(),
                        com.ecommerce.backend.dto.ImagenProductoDto.builder().url("https://example.com/img2.png").principal(true).posicion(1).build()
                ))
                .build();

        org.mockito.ArgumentCaptor<ImagenProducto> imgCaptor = org.mockito.ArgumentCaptor.forClass(ImagenProducto.class);

        productoService.crearProducto(dto);

        verify(imagenProductoRepository, times(2)).save(imgCaptor.capture());
        List<ImagenProducto> savedImgs = imgCaptor.getAllValues();
        assertEquals(2, savedImgs.size());
        assertFalse(savedImgs.get(0).getPrincipal(), "La primera imagen NO debe ser principal si la segunda fue marcada explícitamente");
        assertTrue(savedImgs.get(1).getPrincipal(), "La segunda imagen DEBE ser la principal");
    }
}
