package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ConflictException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;

/** Resolves an exact SKU in one transaction. Inventory and publication are never imported. */
@Service
@RequiredArgsConstructor
public class ProductImportService {
    private final ProductoService products;
    private final ProductoRepository repository;
    private final ObjectMapper mapper;
    private static final Set<String> FIELDS = Set.of("nombre", "descripcion", "precioBase", "costoReferencia", "fichaTecnica", "imagenes");

    public record Snapshot(AdminProductoDetalleDto actual, String revision) {}
    public record Data(@Size(max=220) String nombre, String descripcion,
                       @DecimalMin("0.00") @Digits(integer=8, fraction=2) BigDecimal precioBase,
                       @DecimalMin("0.00") @Digits(integer=8, fraction=2) BigDecimal costoReferencia,
                       FichaProductoDto fichaTecnica,
                       @Size(max=8) List<@NotNull @Valid ImagenProductoDto> imagenes) {}
    public record Resolution(@NotBlank @Size(max=80) String sku, @NotBlank String revision,
                             @NotBlank String modo, @NotNull Set<String> campos, @NotNull @Valid Data datos) {}

    @Transactional(readOnly=true)
    public Snapshot preview(UUID id) {
        var current = products.obtenerPorIdAdmin(id);
        return new Snapshot(current, revision(current));
    }

    @Transactional
    public Snapshot resolve(UUID id, Resolution request) {
        var product = repository.findForGalleryUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Producto no vigente. No se restaurará desde una importación."));
        if (!product.getSku().equals(request.sku())) throw new ConflictException("El SKU exacto no coincide con el producto revisado.");
        var current = products.obtenerPorIdAdmin(id);
        if (!revision(current).equals(request.revision())) throw new ConflictException("El producto cambió después de la comparación. Vuelve a validar y revisar sus diferencias.");
        boolean merge = "completar".equals(request.modo());
        if (!merge && !"actualizar".equals(request.modo())) throw new BadRequestException("Elige completar o actualizar campos seleccionados.");
        if (!FIELDS.containsAll(request.campos()) || (!merge && request.campos().isEmpty())) throw new BadRequestException("Selecciona campos válidos. El stock, SKU, categoría, marca y publicación se conservan.");
        var fields = merge ? FIELDS : request.campos();
        var data = request.datos();
        // Explicit null defaults prevent the general editor's defaults from replacing other fields.
        var patch = AdminProductoDto.builder().activo(null).vendible(null).requiereSerie(null).requiereEnvio(null).imagenes(null).build();
        if (fields.contains("nombre") && (!merge || blank(current.getNombre()))) {
            if (!merge && blank(data.nombre())) throw new BadRequestException("El nombre no puede estar vacío.");
            if (!blank(data.nombre())) patch.setNombre(data.nombre().trim());
        }
        if (fields.contains("descripcion") && (!merge || blank(current.getDescripcion()))) patch.setDescripcion(data.descripcion());
        if (fields.contains("precioBase") && (!merge || current.getPrecioBase()==null)) {
            if (!merge && data.precioBase()==null) throw new BadRequestException("Falta el precio seleccionado.");
            patch.setPrecioBase(data.precioBase());
        }
        if (fields.contains("costoReferencia") && (!merge || current.getCostoReferencia()==null)) {
            if (!merge && data.costoReferencia()==null) throw new BadRequestException("Falta el costo seleccionado; no se interpreta como cero.");
            patch.setCostoReferencia(data.costoReferencia());
        }
        if (fields.contains("fichaTecnica")) {
            if (!merge && data.fichaTecnica()==null) throw new BadRequestException("Falta la ficha técnica seleccionada.");
            if (data.fichaTecnica()!=null) {
                var next=merge ? mergeTechnical(current.getFichaTecnica(),data.fichaTecnica()) : data.fichaTecnica();
                var old=current.getFichaTecnica();
                if (!merge || old==null || !Objects.equals(old.valores(),next.valores()) || !Objects.equals(old.fuenteUrl(),next.fuenteUrl())) patch.setFichaTecnica(next);
            }
        }
        if (fields.contains("imagenes") && !merge) {
            if (data.imagenes()==null) throw new BadRequestException("Falta la galería seleccionada.");
            patch.setImagenes(data.imagenes());
        }
        products.actualizarProducto(id,patch);
        if (merge && data.imagenes()!=null && !data.imagenes().isEmpty()) products.agregarImagenesProducto(id,request.sku(),data.imagenes());
        return preview(id);
    }

    private FichaProductoDto mergeTechnical(FichaProductoDto current, FichaProductoDto incoming) {
        if (current==null) return incoming;
        if (!Objects.equals(current.tipo(),incoming.tipo())) throw new BadRequestException("Las familias técnicas no coinciden. No se combinaron fichas.");
        var values = new LinkedHashMap<String,Object>();
        if (current.valores()!=null) values.putAll(current.valores());
        if (incoming.valores()!=null) incoming.valores().forEach((key,value)->{
            if (missing(values.get(key)) && !missing(value)) values.put(key,value);
        });
        return new FichaProductoDto(current.tipo(),blank(current.fuenteUrl())?incoming.fuenteUrl():current.fuenteUrl(),values);
    }
    private static boolean missing(Object value) { return value==null || value instanceof String s && s.isBlank(); }
    private static boolean blank(String value) { return value==null || value.isBlank(); }

    private String revision(AdminProductoDetalleDto current) {
        try {
            Map<String,Object> data = mapper.convertValue(current,new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});
            data.remove("stockDisponible"); data.remove("atributos");
            byte[] bytes=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsBytes(data);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) { throw new IllegalStateException("No se pudo preparar la comparación del producto.",ex); }
    }
}
