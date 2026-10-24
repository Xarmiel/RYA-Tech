package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.AdminProductoDetalleDto;
import com.ecommerce.backend.dto.AdminProductoDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.service.CategoriaService;
import com.ecommerce.backend.service.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminProductoControllerTest {
    @Test void restauracionPendienteRetorna409EnLugarDeErrorNoConfirmado() throws Exception {
        var controller=new AdminProductoController(productoService,categoriaService,categoriaRepository,marcaRepository);
        var mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new com.ecommerce.backend.exception.GlobalExceptionHandler()).build();
        when(productoService.actualizarFichaProducto(any(),any(),any())).thenThrow(new com.ecommerce.backend.exception.ConflictException("Falta habilitar restauración auditada; no se aplicó el guardado."));
        mvc.perform(patch("/api/v1/admin/productos/"+UUID.randomUUID()+"/ficha-tecnica").contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"CPU-1\",\"fichaTecnica\":{\"tipo\":\"cpu\",\"valores\":{}}}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Falta habilitar restauración auditada; no se aplicó el guardado."));
    }
    @Test void endpointImagenesNoUsaActualizacionComercialCompleta() throws Exception {
        var id=UUID.randomUUID();when(productoService.agregarImagenesProducto(eq(id),eq("CPU-1"),any())).thenReturn(AdminProductoDetalleDto.builder().id(id).sku("CPU-1").build());
        mockMvc.perform(patch("/api/v1/admin/productos/"+id+"/imagenes").contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"CPU-1\",\"imagenes\":[{\"url\":\"https://example.com/cpu.webp\"}]}")).andExpect(status().isOk());
        verify(productoService).agregarImagenesProducto(eq(id),eq("CPU-1"),argThat(list->list.size()==1&&list.getFirst().getUrl().equals("https://example.com/cpu.webp")));
        verify(productoService,never()).actualizarProducto(any(),any());verify(productoService,never()).crearProducto(any());
    }
    @Test void endpointImagenesRechazaListaVaciaYFaltaDeSku() throws Exception {
        var path="/api/v1/admin/productos/"+UUID.randomUUID()+"/imagenes";
        for(var body:List.of("{}","{\"sku\":\"CPU\",\"imagenes\":[]}","{\"imagenes\":[{\"url\":\"https://example.com/a\"}]}"))mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(productoService);
    }
    @Test void actualizaFichaMedianteContratoSeparadoSinUsarGuardadoCompleto() throws Exception {
        UUID id=UUID.randomUUID();
        when(productoService.actualizarFichaProducto(eq(id),eq("CPU-1"),any())).thenReturn(AdminProductoDetalleDto.builder().id(id).sku("CPU-1").build());
        mockMvc.perform(patch("/api/v1/admin/productos/"+id+"/ficha-tecnica").contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"CPU-1\",\"fichaTecnica\":{\"tipo\":\"cpu\",\"valores\":{\"igpu\":false,\"cores\":6}}}")).andExpect(status().isOk());
        verify(productoService).actualizarFichaProducto(eq(id),eq("CPU-1"),argThat(f->Boolean.FALSE.equals(f.valores().get("igpu"))));
        verify(productoService,never()).actualizarProducto(any(),any());verify(productoService,never()).crearProducto(any());
    }
    @Test void actualizarFichaRechazaSkuVacioOFichaAusente() throws Exception {
        String path="/api/v1/admin/productos/"+UUID.randomUUID()+"/ficha-tecnica";
        for(String body:List.of("{}","{\"sku\":\"CPU-1\"}","{\"sku\":\"\",\"fichaTecnica\":{\"tipo\":\"cpu\",\"valores\":{}}}"))mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(productoService);
    }
    @Test
    void rechazaMarcaExistenteSinGuardarDuplicado() {
        when(marcaRepository.existsByNombreIgnoreCaseAndIsDeletedFalse("AMD")).thenReturn(true);
        var controller = new AdminProductoController(productoService, categoriaService, categoriaRepository, marcaRepository);
        var dto = com.ecommerce.backend.dto.CrearMarcaDto.builder().nombre(" AMD ").build();
        org.junit.jupiter.api.Assertions.assertThrows(com.ecommerce.backend.exception.BadRequestException.class, () -> controller.crearMarca(dto));
        verify(marcaRepository, never()).save(any());
    }

    private ProductoService productoService;
    private CategoriaService categoriaService;
    private com.ecommerce.backend.repository.CategoriaRepository categoriaRepository;
    private com.ecommerce.backend.repository.MarcaRepository marcaRepository;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        productoService = mock(ProductoService.class);
        categoriaService = mock(CategoriaService.class);
        categoriaRepository = mock(com.ecommerce.backend.repository.CategoriaRepository.class);
        marcaRepository = mock(com.ecommerce.backend.repository.MarcaRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminProductoController(productoService, categoriaService, categoriaRepository, marcaRepository))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/productos debe listar productos con paginación")
    void testListar() throws Exception {
        UUID prodId = UUID.randomUUID();
        ProductoResumenDto item = ProductoResumenDto.builder()
                .id(prodId)
                .sku("PROD-01")
                .nombre("Monitor Gaming 144Hz")
                .precioBase(new BigDecimal("899.00"))
                .activo(true)
                .vendible(true)
                .stockDisponible(5)
                .build();

        PaginatedResponseDto<ProductoResumenDto> response = PaginatedResponseDto.<ProductoResumenDto>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(productoService.listarProductosAdmin(any(), any(), any(), any(), eq(0), eq(10), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/productos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("PROD-01"))
                .andExpect(jsonPath("$.content[0].nombre").value("Monitor Gaming 144Hz"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/productos debe crear producto y retornar 201 Created")
    void testCrear() throws Exception {
        UUID catId = UUID.randomUUID();
        AdminProductoDto dto = AdminProductoDto.builder()
                .nombre("Teclado Mecanico RGB")
                .sku("KEY-RGB-01")
                .categoriaId(catId)
                .precioBase(new BigDecimal("250.00"))
                .stockInicial(15)
                .estado("draft")
                .build();

        AdminProductoDetalleDto creado = AdminProductoDetalleDto.builder()
                .id(UUID.randomUUID())
                .sku("KEY-RGB-01")
                .nombre("Teclado Mecanico RGB")
                .precioBase(new BigDecimal("250.00"))
                .costoReferencia(new BigDecimal("180.00"))
                .activo(false)
                .vendible(false)
                .stockDisponible(15)
                .build();

        when(productoService.crearProducto(any(AdminProductoDto.class))).thenReturn(creado);

        mockMvc.perform(post("/api/v1/admin/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("KEY-RGB-01"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.costoReferencia").value(180.00));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/productos/{id}/publicar debe publicar producto")
    void testPublicar() throws Exception {
        UUID prodId = UUID.randomUUID();
        AdminProductoDetalleDto detalle = AdminProductoDetalleDto.builder()
                .id(prodId)
                .sku("KEY-RGB-01")
                .activo(true)
                .vendible(true)
                .build();

        when(productoService.cambiarEstado(eq(prodId), eq("publicar"))).thenReturn(detalle);

        mockMvc.perform(patch("/api/v1/admin/productos/" + prodId + "/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.vendible").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/productos/{id}/desactivar debe desactivar producto")
    void testDesactivar() throws Exception {
        UUID prodId = UUID.randomUUID();
        AdminProductoDetalleDto detalle = AdminProductoDetalleDto.builder()
                .id(prodId)
                .sku("KEY-RGB-01")
                .activo(false)
                .vendible(false)
                .build();

        when(productoService.cambiarEstado(eq(prodId), eq("desactivar"))).thenReturn(detalle);

        mockMvc.perform(patch("/api/v1/admin/productos/" + prodId + "/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.vendible").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/admin/productos/categorias debe delegar en CategoriaService")
    void testCrearCategoria() throws Exception {
        com.ecommerce.backend.dto.CrearCategoriaDto dto = com.ecommerce.backend.dto.CrearCategoriaDto.builder()
                .nombre("Refrigeración Líquida")
                .acronimo("AIO")
                .build();

        com.ecommerce.backend.dto.CategoriaDto catDto = com.ecommerce.backend.dto.CategoriaDto.builder()
                .id(UUID.randomUUID())
                .nombre("Refrigeración Líquida")
                .slug("refrigeracion-liquida")
                .build();

        when(categoriaService.crearCategoria(any())).thenReturn(catDto);

        mockMvc.perform(post("/api/v1/admin/productos/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Refrigeración Líquida"))
                .andExpect(jsonPath("$.slug").value("refrigeracion-liquida"));

        verify(categoriaService).crearCategoria(any());
    }

    @Test
    @DisplayName("GET /api/v1/admin/productos/metadata debe retornar categorías y marcas activas")
    void testMetadata() throws Exception {
        when(categoriaRepository.findByIsDeletedFalseOrderByPosicionAsc()).thenReturn(List.of(
                com.ecommerce.backend.entity.Categoria.builder().id(UUID.randomUUID()).nombre("CPUs").slug("cpus").build()
        ));
        when(marcaRepository.findByIsDeletedFalseOrderByNombreAsc()).thenReturn(List.of(
                com.ecommerce.backend.entity.Marca.builder().id(UUID.randomUUID()).nombre("AMD").slug("amd").build()
        ));

        mockMvc.perform(get("/api/v1/admin/productos/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].nombre").value("CPUs"))
                .andExpect(jsonPath("$.marcas[0].nombre").value("AMD"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/productos/{id} debe retornar 204 No Content")
    void testEliminar() throws Exception {
        UUID prodId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/productos/" + prodId))
                .andExpect(status().isNoContent());

        verify(productoService, times(1)).eliminarProducto(prodId);
    }
}
