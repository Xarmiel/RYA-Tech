package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.AjusteStockRequestDto;
import com.ecommerce.backend.dto.AlmacenDto;
import com.ecommerce.backend.dto.InventarioItemDto;
import com.ecommerce.backend.dto.MovimientoInventarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.service.InventarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminInventarioControllerTest {

    private InventarioService inventarioService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        inventarioService = mock(InventarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminInventarioController(inventarioService))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventario debe listar existencias paginadas")
    void testListar() throws Exception {
        UUID exId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        InventarioItemDto item = InventarioItemDto.builder()
                .existenciaId(exId)
                .productoId(prodId)
                .productoNombre("Procesador AMD Ryzen 7 7800X3D")
                .productoSku("AMD-7800X3D")
                .cantidad(10)
                .reservado(2)
                .disponible(8)
                .estadoAlerta("DISPONIBLE")
                .build();

        PaginatedResponseDto<InventarioItemDto> page = PaginatedResponseDto.<InventarioItemDto>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        when(inventarioService.listarInventario(any(), any(), any(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/inventario")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productoNombre").value("Procesador AMD Ryzen 7 7800X3D"))
                .andExpect(jsonPath("$.content[0].cantidad").value(10))
                .andExpect(jsonPath("$.content[0].disponible").value(8));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventario/almacenes debe listar almacenes activos")
    void testListarAlmacenes() throws Exception {
        AlmacenDto alm = AlmacenDto.builder()
                .id(UUID.randomUUID())
                .nombre("Almacén Central")
                .codigo("ALM-CENTRAL")
                .activo(true)
                .build();
        when(inventarioService.listarAlmacenesActivos()).thenReturn(List.of(alm));

        mockMvc.perform(get("/api/v1/admin/inventario/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Almacén Central"))
                .andExpect(jsonPath("$[0].codigo").value("ALM-CENTRAL"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventario/ajuste debe registrar el movimiento correctamente")
    void testRegistrarAjusteExitoso() throws Exception {
        UUID prodId = UUID.randomUUID();
        UUID almId = UUID.randomUUID();

        AjusteStockRequestDto req = AjusteStockRequestDto.builder()
                .operacionId(UUID.randomUUID()).versionEsperada(0)
                .productoId(prodId)
                .almacenId(almId)
                .tipo("ENTRADA")
                .cantidad(5)
                .nota("Ingreso de mercadería por factura F001-1234")
                .build();

        InventarioItemDto updated = InventarioItemDto.builder()
                .existenciaId(UUID.randomUUID())
                .productoId(prodId)
                .productoNombre("AMD Ryzen 7 7800X3D")
                .cantidad(15)
                .reservado(0)
                .disponible(15)
                .estadoAlerta("DISPONIBLE")
                .build();

        when(inventarioService.registrarAjuste(any())).thenReturn(updated);

        mockMvc.perform(post("/api/v1/admin/inventario/ajuste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(15))
                .andExpect(jsonPath("$.disponible").value(15));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventario/ajuste debe rechazar petición si falta la justificación o el tipo es inválido")
    void testRegistrarAjusteValidacion() throws Exception {
        AjusteStockRequestDto invalid = AjusteStockRequestDto.builder()
                .productoId(UUID.randomUUID())
                .almacenId(UUID.randomUUID())
                .tipo("TIPO_INVALIDO")
                .cantidad(5)
                .nota("") // Vacío
                .build();

        mockMvc.perform(post("/api/v1/admin/inventario/ajuste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventario/{id}/kardex debe retornar la lista de movimientos")
    void testObtenerKardex() throws Exception {
        UUID exId = UUID.randomUUID();
        MovimientoInventarioDto mov = MovimientoInventarioDto.builder()
                .id(UUID.randomUUID())
                .existenciaId(exId)
                .tipo("ENTRADA")
                .cantidad(10)
                .nota("Carga inicial")
                .registradoAt(OffsetDateTime.now())
                .build();

        when(inventarioService.obtenerKardex(exId,0,20)).thenReturn(PaginatedResponseDto.<MovimientoInventarioDto>builder().content(List.of(mov)).totalPages(1).build());

        mockMvc.perform(get("/api/v1/admin/inventario/{existenciaId}/kardex", exId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.content[0].cantidad").value(10));
    }
}
