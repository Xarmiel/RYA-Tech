package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AjusteStockRequestDto;
import com.ecommerce.backend.dto.AlmacenDto;
import com.ecommerce.backend.dto.InventarioItemDto;
import com.ecommerce.backend.dto.MovimientoInventarioDto;
import com.ecommerce.backend.dto.PaginatedResponseDto;

import java.util.List;
import java.util.UUID;

public interface InventarioService {

    PaginatedResponseDto<InventarioItemDto> listarInventario(String busqueda, UUID almacenId, String alerta, int page, int size);

    List<AlmacenDto> listarAlmacenesActivos();

    InventarioItemDto registrarAjuste(AjusteStockRequestDto dto);

    List<MovimientoInventarioDto> obtenerKardex(UUID existenciaId);
    PaginatedResponseDto<MovimientoInventarioDto> obtenerKardex(UUID existenciaId, int page, int size);
    InventarioItemDto consultarStock(UUID productoId, UUID almacenId, UUID varianteId);
}
