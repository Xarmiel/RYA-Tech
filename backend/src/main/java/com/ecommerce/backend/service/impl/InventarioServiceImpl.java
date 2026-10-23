package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.Almacen;
import com.ecommerce.backend.entity.Existencia;
import com.ecommerce.backend.entity.ImagenProducto;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.ecommerce.backend.service.InventarioService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final ExistenciaRepository existenciaRepository;
    private final AlmacenRepository almacenRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final InventarioProductLockRepository productLock;
    private static final ObjectMapper AUDIT = new ObjectMapper();
    private static final String ADMIN_OPERATION = "AJUSTE_ADMIN_V2";

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<InventarioItemDto> listarInventario(String busqueda, UUID almacenId, String alerta, int page, int size) {
        validarPagina(page, size);
        Specification<Existencia> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            Join<Existencia, Producto> productoJoin = root.join("producto", JoinType.INNER);
            predicates.add(cb.isFalse(productoJoin.get("isDeleted")));

            if (almacenId != null) {
                predicates.add(cb.equal(root.get("almacenId"), almacenId));
            }

            if (busqueda != null && !busqueda.trim().isBlank()) {
                String term = "%" + busqueda.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(productoJoin.get("nombre")), term),
                        cb.like(cb.lower(productoJoin.get("sku")), term)
                ));
            }

            if (alerta != null && !alerta.trim().isBlank()) {
                Expression<Integer> disponibleExpr = cb.diff(root.get("cantidad"), root.get("reservado"));
                String alertKey = alerta.trim().toLowerCase(Locale.ROOT);
                switch (alertKey) {
                    case "agotado" -> predicates.add(cb.le(disponibleExpr, 0));
                    case "bajo_stock" -> predicates.add(cb.and(cb.gt(disponibleExpr, 0), cb.le(disponibleExpr, 3)));
                    case "alerta", "alert" -> predicates.add(cb.le(disponibleExpr, 3));
                    case "disponible", "active" -> predicates.add(cb.gt(disponibleExpr, 3));
                    default -> throw new BadRequestException("Filtro de inventario inválido");
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("producto.nombre", "id"));
        Page<Existencia> pageResult = existenciaRepository.findAll(spec, pageable);

        if (pageResult.isEmpty()) {
            return PaginatedResponseDto.<InventarioItemDto>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(pageResult.getTotalElements())
                    .totalPages(pageResult.getTotalPages())
                    .first(pageResult.isFirst())
                    .last(pageResult.isLast())
                    .build();
        }

        List<UUID> productIds = pageResult.getContent().stream()
                .map(e -> e.getProducto().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> imagenesMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ImagenProducto> imagenes = imagenProductoRepository.findByProductoIdInAndIsDeletedFalseOrderByPosicionAsc(productIds);
            for (ImagenProducto img : imagenes) {
                UUID pId = img.getProducto().getId();
                if (!imagenesMap.containsKey(pId) || Boolean.TRUE.equals(img.getPrincipal())) {
                    imagenesMap.put(pId, img.getUrl());
                }
            }
        }

        List<InventarioItemDto> items = pageResult.getContent().stream()
                .map(e -> mapearItem(e, imagenesMap.get(e.getProducto().getId())))
                .toList();

        return PaginatedResponseDto.<InventarioItemDto>builder()
                .content(items)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlmacenDto> listarAlmacenesActivos() {
        return almacenRepository.findByActivoTrueAndIsDeletedFalseOrderByNombreAsc().stream()
                .map(a -> AlmacenDto.builder()
                        .id(a.getId())
                        .nombre(a.getNombre())
                        .codigo(a.getCodigo())
                        .direccion(a.getDireccion())
                        .activo(a.getActivo())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public InventarioItemDto registrarAjuste(AjusteStockRequestDto dto) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)
                || !com.ecommerce.backend.config.AdminPermissions.allowed(token.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere una cuenta administradora.");
        }
        String actor = token.getToken().getSubject();
        try { UUID.fromString(actor); } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad administrativa inválida.");
        }
        if (dto.getOperacionId() == null || dto.getVersionEsperada() == null || dto.getVersionEsperada() < -1) {
            throw new BadRequestException("Consulta el stock y proporciona una operación y versión válidas.");
        }
        if (dto.getCantidad() == null || dto.getCantidad() < 0) {
            throw new BadRequestException("La cantidad no puede ser negativa.");
        }
        if (dto.getNota() == null || dto.getNota().trim().isBlank() || dto.getNota().length() > 2000) {
            throw new BadRequestException("El motivo o justificación del ajuste es obligatorio.");
        }

        Producto producto = productLock.bloquear(dto.getProductoId())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + dto.getProductoId()));

        var previous = movimientoInventarioRepository.findByReferenciaTipoAndReferenciaIdAndExistenciaProductoId(ADMIN_OPERATION, dto.getOperacionId(), dto.getProductoId());
        if (previous.isPresent()) {
            JsonNode saved = leerAuditoria(previous.get().getNota());
            if (!saved.path("request").asText().equals(huella(dto, actor))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La operación ya fue utilizada con otros datos.");
            }
            InventarioItemDto response = mapearItem(previous.get().getExistencia(), null);
            response.setCantidad(saved.path("after").asInt());
            response.setReservado(saved.path("reserved").asInt());
            response.setDisponible(response.getCantidad() - response.getReservado());
            response.setEstadoAlerta(response.getDisponible() <= 0 ? "AGOTADO" : response.getDisponible() <= 3 ? "BAJO_STOCK" : "DISPONIBLE");
            response.setVersion(saved.path("version").asInt());
            return response;
        }
        Almacen almacen = almacenRepository.findById(dto.getAlmacenId())
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()) && Boolean.TRUE.equals(a.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado: " + dto.getAlmacenId()));

        Existencia existencia = exacta(dto.getProductoId(), dto.getAlmacenId(), dto.getVarianteId())
                .orElseGet(() -> Existencia.builder()
                        .producto(producto)
                        .almacenId(almacen.getId())
                        .almacen(almacen)
                        .varianteId(dto.getVarianteId())
                        .cantidad(0)
                        .reservado(0)
                        .version(0)
                        .isDeleted(false)
                        .build());

        if (dto.getExistenciaId() != null && !dto.getExistenciaId().equals(existencia.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La existencia no corresponde al producto, variante y almacén elegidos.");
        }
        int expected = existencia.getId() == null ? -1 : existencia.getVersion();
        if (dto.getVersionEsperada() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El stock cambió. Actualiza la consulta antes de confirmar.");
        }
        if (existencia.getId() == null && dto.getVarianteId() != null) {
            throw new BadRequestException("La variante debe tener una existencia registrada antes de utilizarla.");
        }

        int actualCantidad = existencia.getCantidad() != null ? existencia.getCantidad() : 0;
        int reservado = existencia.getReservado() != null ? existencia.getReservado() : 0;
        int nuevaCantidad;
        int deltaMovimiento;
        String tipoMovimiento = dto.getTipo() != null ? dto.getTipo().toUpperCase().trim() : "";

        switch (tipoMovimiento) {
            case "ENTRADA" -> {
                if (dto.getCantidad() <= 0) {
                    throw new BadRequestException("La cantidad de entrada debe ser mayor a 0.");
                }
                if ((long)actualCantidad + dto.getCantidad() > Integer.MAX_VALUE) throw new BadRequestException("La cantidad supera el límite permitido.");
                nuevaCantidad = actualCantidad + dto.getCantidad();
                deltaMovimiento = dto.getCantidad();
            }
            case "SALIDA" -> {
                if (dto.getCantidad() <= 0) {
                    throw new BadRequestException("La cantidad de salida debe ser mayor a 0.");
                }
                if (actualCantidad - dto.getCantidad() < reservado) {
                    throw new BadRequestException(String.format(
                            "No es posible retirar %d unidades. El stock actual es %d y hay %d unidades reservadas por pedidos pendientes.",
                            dto.getCantidad(), actualCantidad, reservado));
                }
                nuevaCantidad = actualCantidad - dto.getCantidad();
                deltaMovimiento = dto.getCantidad();
            }
            case "AJUSTE" -> {
                if (dto.getCantidad() < reservado) {
                    throw new BadRequestException(String.format(
                            "El nuevo stock físico (%d) no puede ser menor a las unidades reservadas actualmente (%d).",
                            dto.getCantidad(), reservado));
                }
                if (dto.getCantidad() == actualCantidad) {
                    throw new BadRequestException("El stock especificado coincide con el stock actual; no hay ajuste que aplicar.");
                }
                nuevaCantidad = dto.getCantidad();
                deltaMovimiento = Math.abs(nuevaCantidad - actualCantidad);
            }
            default -> throw new BadRequestException("Tipo de movimiento inválido: " + tipoMovimiento + ". Debe ser ENTRADA, SALIDA o AJUSTE.");
        }

        existencia.setCantidad(nuevaCantidad);
        existencia = existenciaRepository.saveAndFlush(existencia);

        var audit = AUDIT.createObjectNode();
        audit.put("schema", "rya.inventory-audit.v1");
        audit.put("actor", actor);
        audit.put("before", actualCantidad);
        audit.put("after", nuevaCantidad);
        audit.put("reserved", reservado);
        audit.put("version", existencia.getVersion());
        audit.put("note", dto.getNota().trim());
        audit.put("request", huella(dto, actor));

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .existencia(existencia)
                .tipo(tipoMovimiento)
                .cantidad(deltaMovimiento)
                .referenciaTipo(ADMIN_OPERATION)
                .referenciaId(dto.getOperacionId())
                .nota(audit.toString())
                .registradoAt(OffsetDateTime.now())
                .build();
        movimientoInventarioRepository.save(movimiento);

        String imagenUrl = imagenProductoRepository.findFirstByProductoIdAndPrincipalTrueAndIsDeletedFalse(producto.getId())
                .map(ImagenProducto::getUrl)
                .orElse(null);

        return mapearItem(existencia, imagenUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioDto> obtenerKardex(UUID existenciaId) {
        return obtenerKardex(existenciaId, 0, 20).getContent();
    }

    @Override
    @Transactional(readOnly=true)
    public PaginatedResponseDto<MovimientoInventarioDto> obtenerKardex(UUID existenciaId, int page, int size) {
        validarPagina(page, size);
        if (!existenciaRepository.existsById(existenciaId)) throw new ResourceNotFoundException("Existencia no encontrada");
        var result = movimientoInventarioRepository.findByExistenciaId(existenciaId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registradoAt", "id")));
        return PaginatedResponseDto.<MovimientoInventarioDto>builder().content(result.getContent().stream().map(this::mapearMovimiento).toList())
                .page(page).size(size).totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .first(result.isFirst()).last(result.isLast()).build();
    }

    private MovimientoInventarioDto mapearMovimiento(MovimientoInventario m) {
        var dto = MovimientoInventarioDto.builder().id(m.getId()).existenciaId(m.getExistencia().getId()).tipo(m.getTipo())
                .cantidad(m.getCantidad()).referenciaTipo(m.getReferenciaTipo()).referenciaId(m.getReferenciaId())
                .nota(m.getNota()).registradoAt(m.getRegistradoAt()).build();
        if (ADMIN_OPERATION.equals(m.getReferenciaTipo())) {
            var a = leerAuditoria(m.getNota());
            dto.setActor(a.path("actor").asText());
            dto.setCantidadAnterior(a.path("before").asInt());
            dto.setCantidadNueva(a.path("after").asInt());
            dto.setDelta(dto.getCantidadNueva() - dto.getCantidadAnterior());
            dto.setNota(a.path("note").asText());
        }
        return dto;
    }

    private static JsonNode leerAuditoria(String raw) {
        try { return AUDIT.readTree(raw); } catch (Exception ex) { throw new IllegalStateException("Historial administrativo ilegible", ex); }
    }

    private static String huella(AjusteStockRequestDto d, String actor) {
        String data = String.join("|", actor, String.valueOf(d.getProductoId()), String.valueOf(d.getAlmacenId()),
                String.valueOf(d.getVarianteId()), String.valueOf(d.getExistenciaId()), String.valueOf(d.getVersionEsperada()),
                d.getTipo(), String.valueOf(d.getCantidad()), d.getNota().trim());
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private static void validarPagina(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Paginación inválida: página >= 0, tamaño entre 1 y 100.");
    }

    private Optional<Existencia> exacta(UUID producto, UUID almacen, UUID variante) {
        var rows = existenciaRepository.buscarExacta(producto, almacen, variante);
        if (rows.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Existen registros duplicados. Corrige las existencias antes de operar.");
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly=true)
    public InventarioItemDto consultarStock(UUID productoId, UUID almacenId, UUID varianteId) {
        var producto = productoRepository.findById(productoId).filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        var almacen = almacenRepository.findById(almacenId).filter(a -> Boolean.TRUE.equals(a.getActivo()) && !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Almacén activo no encontrado"));
        var row = exacta(productoId, almacenId, varianteId).orElseGet(() -> Existencia.builder().producto(producto)
                .almacenId(almacenId).almacen(almacen).version(-1).build());
        if (row.getId() == null && varianteId != null) throw new ResourceNotFoundException("Existencia de variante no encontrada");
        return mapearItem(row, null);
    }

    private InventarioItemDto mapearItem(Existencia e, String imagenUrl) {
        int cantidad = e.getCantidad() != null ? e.getCantidad() : 0;
        int reservado = e.getReservado() != null ? e.getReservado() : 0;
        int disponible = Math.max(0, cantidad - reservado);

        String estadoAlerta;
        if (disponible <= 0) {
            estadoAlerta = "AGOTADO";
        } else if (disponible <= 3) {
            estadoAlerta = "BAJO_STOCK";
        } else {
            estadoAlerta = "DISPONIBLE";
        }

        Producto p = e.getProducto();
        Almacen a = e.getAlmacen();
        String almacenNombre = (a != null) ? a.getNombre() : "Almacén no identificado";
        String almacenCodigo = (a != null) ? a.getCodigo() : null;

        return InventarioItemDto.builder()
                .varianteId(e.getVarianteId())
                .version(e.getVersion())
                .existenciaId(e.getId())
                .productoId(p != null ? p.getId() : null)
                .productoNombre(p != null ? p.getNombre() : "Producto")
                .productoSku(p != null ? p.getSku() : "")
                .productoSlug(p != null ? p.getSlug() : null)
                .marca(p != null && p.getMarca() != null ? p.getMarca().getNombre() : null)
                .categoria(p != null && p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .imagenPrincipal(imagenUrl)
                .almacenId(e.getAlmacenId())
                .almacenNombre(almacenNombre)
                .almacenCodigo(almacenCodigo)
                .cantidad(cantidad)
                .reservado(reservado)
                .disponible(disponible)
                .estadoAlerta(estadoAlerta)
                .build();
    }
}
