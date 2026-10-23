package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.FiltroAtributoDto;
import com.ecommerce.backend.dto.FiltrosDisponiblesDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;
import com.ecommerce.backend.dto.ProductoResumenDto;
import com.ecommerce.backend.service.FiltroService;
import com.ecommerce.backend.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductoControllerContractTest {

    private ProductoService productoService;
    private FiltroService filtroService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productoService = mock(ProductoService.class);
        filtroService = mock(FiltroService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductoController(productoService, filtroService))
                .build();
    }

    @Test
    void publicaLaPaginaConElContratoDelBackend() throws Exception {
        ProductoResumenDto producto = ProductoResumenDto.builder()
                .id(UUID.randomUUID())
                .sku("CPU-AMD-TEST")
                .nombre("CPU de prueba")
                .slug("cpu-de-prueba")
                .precioBase(new BigDecimal("999.90"))
                .categoria("Procesadores")
                .stockDisponible(2)
                .build();
        PaginatedResponseDto<ProductoResumenDto> pagina = PaginatedResponseDto.<ProductoResumenDto>builder()
                .content(List.of(producto))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        when(productoService.listarProductos(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(Integer.class), any(Integer.class), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/productos")
                        .param("filtro.socket_cpu", "AM5")
                        .param("filtro.socket_cpu", "AM4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("CPU de prueba"))
                .andExpect(jsonPath("$.content[0].precioBase").value(999.90))
                .andExpect(jsonPath("$.totalElements").value(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> filtros = ArgumentCaptor.forClass(Map.class);
        verify(productoService).listarProductos(any(), any(), any(), any(), any(), any(), any(), any(),
                filtros.capture(), any(Integer.class), any(Integer.class), any(), any());
        assertEquals(List.of("AM5", "AM4"), filtros.getValue().get("socket_cpu"));
    }

    @Test
    void publicaFiltrosTecnicosDinamicos() throws Exception {
        when(filtroService.obtenerFiltrosDisponibles("procesadores")).thenReturn(FiltrosDisponiblesDto.builder()
                .atributos(List.of(FiltroAtributoDto.builder()
                        .codigo("socket_cpu")
                        .nombre("Socket")
                        .tipoDato("TEXTO")
                        .valores(List.of("AM5"))
                        .build()))
                .build());

        mockMvc.perform(get("/api/v1/productos/filtros").param("categoria", "procesadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atributos[0].codigo").value("socket_cpu"))
                .andExpect(jsonPath("$.atributos[0].valores[0]").value("AM5"));
    }
}
