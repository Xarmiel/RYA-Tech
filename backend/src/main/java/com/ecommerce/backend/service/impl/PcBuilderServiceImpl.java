package com.ecommerce.backend.service.impl;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.mapper.ProductoMapper;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.PcBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PcBuilderServiceImpl implements PcBuilderService {

    private final TipoComponenteRepository tipoComponenteRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final ExistenciaRepository existenciaRepository;
    private final EspecificacionCpuRepository especificacionCpuRepository;
    private final RevisionPlacaRepository revisionPlacaRepository;
    private final VersionBiosRepository versionBiosRepository;
    private final CompatibilidadCpuPlacaRepository compatibilidadCpuPlacaRepository;
    private final ValorAtributoProductoRepository valorAtributoProductoRepository;
    private final ProductoMapper productoMapper;

    @Override
    public PcBuilderInicialDto obtenerDatosIniciales() {
        List<TipoComponenteResumenDto> tipos = tipoComponenteRepository.findByIsDeletedFalseOrderByCodigoAsc().stream()
                .map(tipo -> TipoComponenteResumenDto.builder()
                        .id(tipo.getId())
                        .code(tipo.getCodigo())
                        .name(tipo.getNombre())
                        .obligatorio(tipo.getObligatorio())
                        .build())
                .toList();
        if (tipos.isEmpty()) {
            tipos = categoriaRepository.findByIsDeletedFalseOrderByPosicionAsc().stream()
                    .map(categoria -> TipoComponenteResumenDto.builder()
                            .id(categoria.getId())
                            .code(categoria.getAcronimo() != null && !categoria.getAcronimo().isBlank()
                                    ? categoria.getAcronimo() : categoria.getSlug())
                            .name(categoria.getNombre())
                            .obligatorio(false)
                            .build())
                    .toList();
        }
        return PcBuilderInicialDto.builder()
                .componentTypes(tipos)
                .profileFamilies(List.of())
                .resolutions(List.of())
                .build();
    }

    @Override
    public ComponenteTipoDto listarComponentesPorTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            throw new BadRequestException("El tipo de componente no puede ser nulo o vacío");
        }

        String tipoNorm = tipo.trim();

        // 1. Buscar en rya.tipos_componente por código
        Optional<TipoComponente> tipoCompOpt = tipoComponenteRepository.findByCodigoIgnoreCaseAndIsDeletedFalse(tipoNorm);

        // 2. Buscar productos asociados por categoría (slug o acrónimo) o tipo
        List<Producto> productos = new ArrayList<>();
        String nombreTipo = tipoNorm;
        UUID tipoId = null;
        boolean obligatorio = true;

        if (tipoCompOpt.isPresent()) {
            TipoComponente tc = tipoCompOpt.get();
            tipoId = tc.getId();
            nombreTipo = tc.getNombre();
            obligatorio = Boolean.TRUE.equals(tc.getObligatorio());

            // Buscar por categoría vinculada a dicho componente
            productos = productoRepository.findActivosPorCategoriaAcronimo(tc.getCodigo());
            if (productos.isEmpty()) {
                productos = productoRepository.findActivosPorCategoriaSlug(tc.getCodigo().toLowerCase());
            }
        }

        // Si no se encontraron por código, buscar directamente por slug de categoría
        if (productos.isEmpty()) {
            Optional<Categoria> catOpt = categoriaRepository.findBySlugAndIsDeletedFalse(tipoNorm.toLowerCase());
            if (catOpt.isEmpty()) {
                catOpt = categoriaRepository.findByAcronimoIgnoreCaseAndIsDeletedFalse(tipoNorm);
            }
            if (catOpt.isPresent()) {
                Categoria cat = catOpt.get();
                nombreTipo = cat.getNombre();
                productos = productoRepository.findActivosPorCategoriaId(cat.getId());
            }
        }

        // Mapear con imágenes principales y stock para evitar N+1
        List<ProductoResumenDto> productosDto = mapearProductosConStockEImagenes(productos);

        return ComponenteTipoDto.builder()
                .id(tipoId)
                .codigo(tipoNorm.toUpperCase())
                .nombre(nombreTipo)
                .obligatorio(obligatorio)
                .productos(productosDto)
                .build();
    }

    @Override
    public List<ProductoResumenDto> obtenerComponentesCompatibles(String tipo, List<UUID> componentesActuales) {
        ComponenteTipoDto tipoDto = listarComponentesPorTipo(tipo);
        if (componentesActuales == null || componentesActuales.isEmpty()) {
            return tipoDto.getProductos();
        }

        List<ProductoResumenDto> candidatos = tipoDto.getProductos();

        // Si solicitan placas compatibles y hay un CPU en componentesActuales
        UUID cpuId = null;
        for (UUID compId : componentesActuales) {
            if (especificacionCpuRepository.existsById(compId)) {
                cpuId = compId;
                break;
            }
        }

        if (cpuId != null && ("PLACA".equalsIgnoreCase(tipo) || "MOTHERBOARD".equalsIgnoreCase(tipo) || "PLACAS-MADRE".equalsIgnoreCase(tipo))) {
            List<CompatibilidadCpuPlaca> soportadas = compatibilidadCpuPlacaRepository.findPlacasSoportadasPorCpuId(cpuId);
            Set<UUID> productosPlacaPermitidos = new HashSet<>();
            for (CompatibilidadCpuPlaca rel : soportadas) {
                if (rel.getRevisionPlaca() != null) {
                    productosPlacaPermitidos.add(rel.getRevisionPlaca().getProductoId());
                }
            }

            candidatos = tipoDto.getProductos().stream()
                    .filter(p -> productosPlacaPermitidos.contains(p.getId()))
                    .toList();
        }

        // Evaluar cada candidato con las fichas técnicas disponibles. Solo se descartan
        // incompatibilidades demostradas; una selección todavía incompleta puede quedar POR_VERIFICAR.
        return candidatos.stream()
                .filter(candidato -> noIntroduceIncompatibilidad(componentesActuales, candidato.getId()))
                .toList();
    }

    private boolean noIntroduceIncompatibilidad(List<UUID> actuales, UUID candidatoId) {
        try {
            LinkedHashMap<String, UUID> seleccion = new LinkedHashMap<>();
            List<UUID> ids = new ArrayList<>(actuales);
            ids.add(candidatoId);
            for (UUID id : ids) {
                Producto producto = productoRepository.findConDetallesPorId(id).orElse(null);
                if (producto == null || producto.getCategoria() == null) continue;
                String codigo = producto.getCategoria().getAcronimo();
                if (codigo == null || codigo.isBlank()) codigo = producto.getCategoria().getSlug();
                seleccion.put(normalizarTipo(codigo), id);
            }
            ValidarCambioResponseDto resultado = validarCambio(ValidarCambioRequestDto.builder()
                    .cpuId(valorTipo(seleccion, "CPU", "PROCESADOR", "PROCESADORES"))
                    .placaId(valorTipo(seleccion, "PLACA", "MB", "MOTHERBOARD", "PLACAS-MADRE", "PLACASMADRE"))
                    .ramId(valorTipo(seleccion, "RAM", "MEMORIA", "MEMORIA-RAM", "MEMORIARAM"))
                    .gpuId(valorTipo(seleccion, "GPU", "TARJETA-GRAFICA", "TARJETAS-GRAFICAS", "TARJETAGRAFICA"))
                    .fuenteId(valorTipo(seleccion, "PSU", "FUENTE", "FUENTES-DE-PODER", "FUENTEDEPODER"))
                    .gabineteId(valorTipo(seleccion, "CASE", "GABINETE", "GABINETES"))
                    .refrigeracionId(valorTipo(seleccion, "COOLER", "REFRIGERACION", "ENFRIAMIENTO-LIQUIDO"))
                    .componentesActuales(new ArrayList<>(new LinkedHashSet<>(seleccion.values())))
                    .build());
            return !"INCOMPATIBLE".equalsIgnoreCase(resultado.getEstadoCompatibilidad());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private UUID valorTipo(Map<String, UUID> seleccion, String... codigos) {
        for (String codigo : codigos) {
            UUID id = seleccion.get(normalizarTipo(codigo));
            if (id != null) return id;
        }
        return null;
    }

    private String normalizarTipo(String valor) {
        return Optional.ofNullable(valor).orElse("")
                .trim().toUpperCase(Locale.ROOT).replace('_', '-').replaceAll("\\s+", "");
    }

    @Override
    public ValidarCambioResponseDto validarCambio(ValidarCambioRequestDto request) {
        ValidarCambioResponseDto plataforma = validarCpuPlaca(request);
        List<ResultadoRegla> reglas = validarReglasTecnicas(request);
        return combinarResultados(plataforma, reglas);
    }

    private ValidarCambioResponseDto validarCpuPlaca(ValidarCambioRequestDto request) {
        if (request == null) {
            throw new BadRequestException("El cuerpo de la solicitud no puede estar vacío");
        }

        UUID cpuId = request.getCpuId();
        UUID placaId = request.getPlacaId();
        UUID placaRevisionId = request.getPlacaRevisionId();

        // Una selección incompleta nunca puede declararse como compatibilidad verificada.
        if (cpuId == null || (placaId == null && placaRevisionId == null)) {
            return ValidarCambioResponseDto.builder()
                    .esCompatible(false)
                    .estadoCompatibilidad("POR_VERIFICAR")
                    .mensajes(List.of("Selecciona una CPU y una placa madre con revisión identificada para validar socket y BIOS."))
                    .requiereActualizacionBios(false)
                    .build();
        }

        // 1. Obtener especificaciones del CPU
        EspecificacionCpu cpu = especificacionCpuRepository.findConProductoPorId(cpuId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron especificaciones técnicas de CPU para el producto: " + cpuId));

        // 2. Resolver la revisión de placa
        RevisionPlaca revisionPlaca = null;
        if (placaRevisionId != null) {
            revisionPlaca = revisionPlacaRepository.findById(placaRevisionId)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la revisión de placa: " + placaRevisionId));
        } else {
            List<RevisionPlaca> revisiones = revisionPlacaRepository.findByProductoId(placaId);
            if (!revisiones.isEmpty()) {
                revisionPlaca = revisiones.get(0); // Tomar la revisión principal registrada
            }
        }

        if (revisionPlaca == null) {
            // No existe ficha de revisión técnica en rya.revisiones_placa
            return ValidarCambioResponseDto.builder()
                    .esCompatible(false)
                    .estadoCompatibilidad("POR_VERIFICAR")
                    .mensajes(List.of("No se encontraron revisiones técnicas en rya.revisiones_placa para la placa madre indicada. Requiere verificación previa."))
                    .requiereActualizacionBios(false)
                    .build();
        }

        // 3. Regla estricta: Consultar evidencia oficial en rya.compatibilidad_cpu_placa
        Optional<CompatibilidadCpuPlaca> compatOpt = compatibilidadCpuPlacaRepository.findConDetalles(cpu.getProductoId(), revisionPlaca.getId());

        if (compatOpt.isPresent()) {
            CompatibilidadCpuPlaca compat = compatOpt.get();
            String soporte = compat.getSoporte();

            if ("NO_SOPORTADO".equalsIgnoreCase(soporte)) {
                return ValidarCambioResponseDto.builder()
                        .esCompatible(false)
                        .estadoCompatibilidad("INCOMPATIBLE")
                        .mensajes(List.of("Incompatible: El fabricante indica expresamente que esta CPU no es soportada por esta placa madre (" +
                                (compat.getCondiciones() != null ? compat.getCondiciones() : "Sin soporte oficial") + ")."))
                        .requiereActualizacionBios(false)
                        .condiciones(compat.getCondiciones())
                        .build();
            }

            if ("POR_VERIFICAR".equalsIgnoreCase(soporte)) {
                return ValidarCambioResponseDto.builder()
                        .esCompatible(false)
                        .estadoCompatibilidad("POR_VERIFICAR")
                        .mensajes(List.of("El soporte de esta CPU en la placa aún no ha sido certificado o se encuentra pendiente de verificación por el fabricante."))
                        .requiereActualizacionBios(false)
                        .condiciones(compat.getCondiciones())
                        .build();
            }

            if ("SOPORTADO".equalsIgnoreCase(soporte)) {
                VersionBios biosMinima = compat.getBiosMinima();
                String nombreBiosMinima = biosMinima != null ? biosMinima.getVersion() : null;

                // Una BIOS mínima sin versión instalada conocida implica compatibilidad condicional.
                if (biosMinima != null && (request.getBiosInstalada() == null || request.getBiosInstalada().isBlank())) {
                    return ValidarCambioResponseDto.builder()
                            .esCompatible(true)
                            .estadoCompatibilidad("CONDICIONAL")
                            .requiereActualizacionBios(false)
                            .biosMinimaRequerida(nombreBiosMinima)
                            .condiciones(compat.getCondiciones())
                            .mensajes(List.of("La combinación tiene soporte oficial, pero debes confirmar que la placa use la BIOS " + nombreBiosMinima + " o una versión posterior."))
                            .build();
                }

                if (biosMinima != null) {
                    Optional<VersionBios> biosInstaladaOpt = versionBiosRepository.findByPlacaRevisionIdAndVersionIgnoreCase(
                            revisionPlaca.getId(), request.getBiosInstalada().trim());

                    if (biosInstaladaOpt.isEmpty()) {
                        return ValidarCambioResponseDto.builder()
                                .esCompatible(false)
                                .estadoCompatibilidad("POR_VERIFICAR")
                                .requiereActualizacionBios(false)
                                .biosMinimaRequerida(nombreBiosMinima)
                                .condiciones(compat.getCondiciones())
                                .mensajes(List.of("La versión de BIOS indicada no está registrada para esta revisión de placa; no es posible verificarla."))
                                .build();
                    }

                    if (biosInstaladaOpt.get().getOrden() < biosMinima.getOrden()) {
                        return ValidarCambioResponseDto.builder()
                                .esCompatible(true)
                                .estadoCompatibilidad("CONDICIONAL")
                                .requiereActualizacionBios(true)
                                .biosMinimaRequerida(nombreBiosMinima)
                                .condiciones(compat.getCondiciones())
                                .mensajes(List.of("Compatible bajo condición: Requiere actualizar la BIOS instalada (" +
                                        request.getBiosInstalada() + ") a la versión mínima " + nombreBiosMinima + " o superior."))
                                .build();
                    }
                }

                List<String> avisos = new ArrayList<>();
                avisos.add("Combinación de CPU y Placa Madre VERIFICADA con soporte oficial del fabricante.");
                if (nombreBiosMinima != null) {
                    avisos.add("Requiere versión de BIOS mínima: " + nombreBiosMinima + ".");
                }

                return ValidarCambioResponseDto.builder()
                        .esCompatible(true)
                        .estadoCompatibilidad("VERIFICADA")
                        .requiereActualizacionBios(false)
                        .biosMinimaRequerida(nombreBiosMinima)
                        .condiciones(compat.getCondiciones())
                        .mensajes(avisos)
                        .build();
            }
        }

        // Si no hay registro en la tabla de compatibilidad oficial:
        // Comparar sockets físicos
        if (!cpu.getSocket().equalsIgnoreCase(revisionPlaca.getSocket())) {
            return ValidarCambioResponseDto.builder()
                    .esCompatible(false)
                    .estadoCompatibilidad("INCOMPATIBLE")
                    .mensajes(List.of("Incompatibilidad física de socket: CPU usa " + cpu.getSocket() +
                            " y la placa madre usa " + revisionPlaca.getSocket() + "."))
                    .requiereActualizacionBios(false)
                    .build();
        }

        // Regla obligatoria: "No declares compatibilidad únicamente por socket si existen tablas de compatibilidad y BIOS"
        return ValidarCambioResponseDto.builder()
                .esCompatible(false)
                .estadoCompatibilidad("POR_VERIFICAR")
                .mensajes(List.of("El socket físico coincide (" + cpu.getSocket() +
                        "), pero no existe registro de certificación en la tabla de compatibilidad y BIOS (rya.compatibilidad_cpu_placa). No se declara compatibilidad únicamente por socket."))
                .requiereActualizacionBios(false)
                .build();
    }

    private List<ResultadoRegla> validarReglasTecnicas(ValidarCambioRequestDto request) {
        List<ResultadoRegla> resultados = new ArrayList<>();
        Set<UUID> seleccionados = idsSeleccionados(request);

        Set<UUID> bloqueados = new LinkedHashSet<>(Optional.ofNullable(request.getComponentesBloqueados()).orElse(List.of()));
        Set<UUID> retirados = new LinkedHashSet<>(bloqueados);
        retirados.removeAll(seleccionados);
        if (!retirados.isEmpty()) {
            resultados.add(ResultadoRegla.incompatible("COMPONENTES_BLOQUEADOS",
                    "El cambio intenta retirar o sustituir componentes bloqueados: " + retirados + "."));
        }

        List<UUID> idsConAtributos = Stream.of(
                        request.getPlacaId(), request.getRamId(), request.getGpuId(), request.getFuenteId(),
                        request.getGabineteId(), request.getRefrigeracionId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Map<String, ValorAtributoProducto>> atributos = cargarAtributos(idsConAtributos);

        validarRamYPlaca(request, atributos, resultados);
        validarRefrigeracion(request, atributos, resultados);
        validarGabinete(request, atributos, resultados);
        validarFuente(request, atributos, resultados);
        validarSalidaVideo(request, resultados);
        return resultados;
    }

    private void validarRamYPlaca(ValidarCambioRequestDto request,
                                  Map<UUID, Map<String, ValorAtributoProducto>> atributos,
                                  List<ResultadoRegla> resultados) {
        if (request.getRamId() == null || request.getPlacaId() == null) return;

        Map<String, ValorAtributoProducto> ram = atributos.getOrDefault(request.getRamId(), Map.of());
        Map<String, ValorAtributoProducto> placa = atributos.getOrDefault(request.getPlacaId(), Map.of());
        String tipoRam = texto(ram, "tipo_ram");
        String tipoPlaca = texto(placa, "tipo_ram_placa");
        if (tipoRam == null || tipoPlaca == null) {
            resultados.add(ResultadoRegla.porVerificar("RAM_TIPO",
                    "Falta el tipo de memoria de la RAM o de la placa madre."));
        } else if (!igualTecnico(tipoRam, tipoPlaca)) {
            resultados.add(ResultadoRegla.incompatible("RAM_TIPO",
                    "La RAM usa " + tipoRam + " y la placa madre requiere " + tipoPlaca + "."));
        } else {
            resultados.add(ResultadoRegla.verificada("RAM_TIPO", "El tipo de RAM coincide con la placa madre."));
        }

        compararMaximo(ram, "capacidad_ram_gb", placa, "capacidad_ram_max_placa_gb",
                "RAM_CAPACIDAD", "La capacidad de RAM supera el máximo admitido por la placa.", resultados, false);
        compararMaximo(ram, "modulos_ram", placa, "ranuras_ram_placa",
                "RAM_RANURAS", "La cantidad de módulos RAM supera las ranuras de la placa.", resultados, false);
        compararMaximo(ram, "velocidad_ram_mt_s", placa, "velocidad_ram_max_placa_mt_s",
                "RAM_VELOCIDAD", "La RAM funcionará limitada a la velocidad máxima de la placa.", resultados, true);
    }

    private void validarRefrigeracion(ValidarCambioRequestDto request,
                                      Map<UUID, Map<String, ValorAtributoProducto>> atributos,
                                      List<ResultadoRegla> resultados) {
        if (request.getRefrigeracionId() == null) return;
        Map<String, ValorAtributoProducto> cooler = atributos.getOrDefault(request.getRefrigeracionId(), Map.of());

        if (request.getCpuId() != null) {
            EspecificacionCpu cpu = especificacionCpuRepository.findConProductoPorId(request.getCpuId()).orElse(null);
            String sockets = texto(cooler, "sockets_cooler");
            if (cpu == null || sockets == null) {
                resultados.add(ResultadoRegla.porVerificar("COOLER_SOCKET",
                        "Falta la ficha de CPU o la lista de sockets compatibles del cooler."));
            } else if (!contieneOpcion(sockets, cpu.getSocket())) {
                resultados.add(ResultadoRegla.incompatible("COOLER_SOCKET",
                        "El cooler no declara soporte para el socket " + cpu.getSocket() + "."));
            } else {
                resultados.add(ResultadoRegla.verificada("COOLER_SOCKET", "El cooler admite el socket del procesador."));
            }

            BigDecimal capacidad = numero(cooler, "capacidad_termica_cooler_w");
            BigDecimal potenciaMaxima = cpu != null ? cpu.getPotenciaMaximaW() : null;
            if (capacidad == null || potenciaMaxima == null) {
                resultados.add(ResultadoRegla.porVerificar("COOLER_TERMICA",
                        "Falta la potencia máxima documentada de la CPU o la capacidad térmica del cooler."));
            } else if (capacidad.compareTo(potenciaMaxima) < 0) {
                resultados.add(ResultadoRegla.incompatible("COOLER_TERMICA",
                        "La capacidad térmica del cooler es menor que la potencia máxima documentada de la CPU."));
            } else {
                resultados.add(ResultadoRegla.verificada("COOLER_TERMICA", "La capacidad térmica del cooler es suficiente."));
            }
        }
    }

    private void validarGabinete(ValidarCambioRequestDto request,
                                 Map<UUID, Map<String, ValorAtributoProducto>> atributos,
                                 List<ResultadoRegla> resultados) {
        if (request.getGabineteId() == null) return;
        Map<String, ValorAtributoProducto> gabinete = atributos.getOrDefault(request.getGabineteId(), Map.of());

        if (request.getPlacaId() != null) {
            validarOpcionIncluida(atributos.getOrDefault(request.getPlacaId(), Map.of()), "formato_placa",
                    gabinete, "formatos_placa_gabinete", "GABINETE_PLACA",
                    "El gabinete no admite el formato de la placa madre.", resultados);
        }
        if (request.getGpuId() != null) {
            Map<String, ValorAtributoProducto> gpu = atributos.getOrDefault(request.getGpuId(), Map.of());
            compararMaximo(gpu, "largo_gpu_mm", gabinete, "largo_gpu_max_mm", "GABINETE_GPU_LARGO",
                    "La tarjeta gráfica excede el largo máximo del gabinete.", resultados, false);
            compararMaximo(gpu, "ancho_gpu_slots", gabinete, "ancho_gpu_max_slots", "GABINETE_GPU_SLOTS",
                    "La tarjeta gráfica ocupa más slots de los admitidos por el gabinete.", resultados, false);
        }
        if (request.getFuenteId() != null) {
            Map<String, ValorAtributoProducto> fuente = atributos.getOrDefault(request.getFuenteId(), Map.of());
            validarOpcionIncluida(fuente, "formato_psu", gabinete, "formatos_psu_gabinete", "GABINETE_PSU_FORMATO",
                    "El gabinete no admite el formato de la fuente de poder.", resultados);
            compararMaximo(fuente, "largo_psu_mm", gabinete, "largo_psu_max_mm", "GABINETE_PSU_LARGO",
                    "La fuente de poder excede el largo máximo del gabinete.", resultados, false);
        }
        if (request.getRefrigeracionId() != null) {
            Map<String, ValorAtributoProducto> cooler = atributos.getOrDefault(request.getRefrigeracionId(), Map.of());
            BigDecimal altura = numero(cooler, "altura_cooler_mm");
            if (altura != null) {
                compararMaximo(cooler, "altura_cooler_mm", gabinete, "altura_cooler_max_mm", "GABINETE_COOLER_ALTURA",
                        "El disipador excede la altura máxima del gabinete.", resultados, false);
            }
            BigDecimal radiador = numero(cooler, "tamano_radiador_mm");
            if (radiador != null) {
                String soportados = texto(gabinete, "radiadores_soportados_gabinete");
                if (soportados == null) {
                    resultados.add(ResultadoRegla.porVerificar("GABINETE_RADIADOR",
                            "El gabinete no tiene registrados los tamaños de radiador admitidos."));
                } else if (!contieneOpcion(soportados, radiador.stripTrailingZeros().toPlainString())) {
                    resultados.add(ResultadoRegla.incompatible("GABINETE_RADIADOR",
                            "El gabinete no admite un radiador de " + radiador.stripTrailingZeros().toPlainString() + " mm."));
                } else {
                    resultados.add(ResultadoRegla.verificada("GABINETE_RADIADOR", "El radiador cabe en el gabinete."));
                }
            }
        }
    }

    private void validarFuente(ValidarCambioRequestDto request,
                               Map<UUID, Map<String, ValorAtributoProducto>> atributos,
                               List<ResultadoRegla> resultados) {
        if (request.getFuenteId() == null) return;
        Map<String, ValorAtributoProducto> fuente = atributos.getOrDefault(request.getFuenteId(), Map.of());
        BigDecimal potenciaFuente = numero(fuente, "potencia_psu_w");
        BigDecimal consumo = BigDecimal.ZERO;
        boolean consumoCompleto = true;

        if (request.getCpuId() != null) {
            EspecificacionCpu cpu = especificacionCpuRepository.findConProductoPorId(request.getCpuId()).orElse(null);
            if (cpu == null || cpu.getPotenciaMaximaW() == null) consumoCompleto = false;
            else consumo = consumo.add(cpu.getPotenciaMaximaW());
        }
        if (request.getGpuId() != null) {
            BigDecimal potenciaGpu = numero(atributos.getOrDefault(request.getGpuId(), Map.of()), "potencia_gpu_w");
            if (potenciaGpu == null) consumoCompleto = false;
            else consumo = consumo.add(potenciaGpu);
        }
        if (request.getRefrigeracionId() != null) {
            BigDecimal consumoCooler = numero(atributos.getOrDefault(request.getRefrigeracionId(), Map.of()), "consumo_cooler_w");
            if (consumoCooler == null) consumoCompleto = false;
            else consumo = consumo.add(consumoCooler);
        }

        if (potenciaFuente == null || !consumoCompleto || consumo.signum() == 0) {
            resultados.add(ResultadoRegla.porVerificar("PSU_POTENCIA",
                    "Falta potencia documentada para calcular y verificar la fuente con margen seguro."));
        } else {
            int recomendada = calcularPsuEstandar(consumo.multiply(BigDecimal.valueOf(1.25))
                    .add(BigDecimal.valueOf(50)).setScale(0, RoundingMode.CEILING).intValue());
            if (potenciaFuente.compareTo(BigDecimal.valueOf(recomendada)) < 0) {
                resultados.add(ResultadoRegla.incompatible("PSU_POTENCIA",
                        "La fuente es de " + potenciaFuente.stripTrailingZeros().toPlainString()
                                + " W y la configuración requiere al menos " + recomendada + " W."));
            } else {
                resultados.add(ResultadoRegla.verificada("PSU_POTENCIA",
                        "La fuente cumple la potencia recomendada de " + recomendada + " W."));
            }
        }

        if (request.getGpuId() != null) {
            Map<String, ValorAtributoProducto> gpu = atributos.getOrDefault(request.getGpuId(), Map.of());
            String conector = texto(gpu, "conector_energia_gpu");
            BigDecimal cantidad = numero(gpu, "cantidad_conectores_gpu");
            if (conector == null || cantidad == null) {
                resultados.add(ResultadoRegla.porVerificar("PSU_CONECTOR_GPU",
                        "Faltan los conectores eléctricos requeridos por la GPU."));
            } else {
                String codigoPsu = normalizarToken(conector).contains("12VHPWR") || normalizarToken(conector).contains("12V2X6")
                        ? "conectores_12vhpwr" : "conectores_pcie_8pin";
                BigDecimal disponibles = numero(fuente, codigoPsu);
                if (disponibles == null) {
                    resultados.add(ResultadoRegla.porVerificar("PSU_CONECTOR_GPU",
                            "La fuente no tiene documentada la cantidad de conectores " + conector + "."));
                } else if (disponibles.compareTo(cantidad) < 0) {
                    resultados.add(ResultadoRegla.incompatible("PSU_CONECTOR_GPU",
                            "La fuente no dispone de suficientes conectores para la GPU."));
                } else {
                    resultados.add(ResultadoRegla.verificada("PSU_CONECTOR_GPU", "La fuente incluye los conectores requeridos por la GPU."));
                }
            }
        }
    }

    private void validarSalidaVideo(ValidarCambioRequestDto request, List<ResultadoRegla> resultados) {
        if (request.getCpuId() == null || (request.getGpuId() == null
                && request.getFuenteId() == null && request.getGabineteId() == null)) return;
        EspecificacionCpu cpu = especificacionCpuRepository.findConProductoPorId(request.getCpuId()).orElse(null);
        if (cpu == null || cpu.getTieneGraficosIntegrados() == null) {
            resultados.add(ResultadoRegla.porVerificar("SALIDA_VIDEO", "No se conoce si el procesador integra gráficos."));
        } else if (!cpu.getTieneGraficosIntegrados() && request.getGpuId() == null) {
            resultados.add(ResultadoRegla.condicional("SALIDA_VIDEO",
                    "El procesador no tiene gráficos integrados; debes añadir una tarjeta gráfica dedicada."));
        }
    }

    private ValidarCambioResponseDto combinarResultados(ValidarCambioResponseDto plataforma,
                                                         List<ResultadoRegla> reglas) {
        String estado = plataforma.getEstadoCompatibilidad();
        List<String> mensajes = new ArrayList<>(Optional.ofNullable(plataforma.getMensajes()).orElse(List.of()));
        List<Map<String, String>> detalleReglas = new ArrayList<>();
        for (ResultadoRegla regla : reglas) {
            if (prioridadEstado(regla.estado()) > prioridadEstado(estado)) estado = regla.estado();
            mensajes.add(regla.mensaje());
            detalleReglas.add(Map.of("codigo", regla.codigo(), "estado", regla.estado(), "mensaje", regla.mensaje()));
        }
        Map<String, Object> detalles = new LinkedHashMap<>(Optional.ofNullable(plataforma.getDetallesTecnicos()).orElse(Map.of()));
        detalles.put("reglas", detalleReglas);
        boolean compatible = "VERIFICADA".equals(estado) || "CONDICIONAL".equals(estado);
        return ValidarCambioResponseDto.builder()
                .esCompatible(compatible)
                .estadoCompatibilidad(estado)
                .mensajes(mensajes)
                .requiereActualizacionBios(plataforma.getRequiereActualizacionBios())
                .biosMinimaRequerida(plataforma.getBiosMinimaRequerida())
                .condiciones(plataforma.getCondiciones())
                .detallesTecnicos(detalles)
                .build();
    }

    private int prioridadEstado(String estado) {
        return switch (Optional.ofNullable(estado).orElse("POR_VERIFICAR")) {
            case "INCOMPATIBLE" -> 4;
            case "POR_VERIFICAR" -> 3;
            case "CONDICIONAL" -> 2;
            default -> 1;
        };
    }

    private Map<UUID, Map<String, ValorAtributoProducto>> cargarAtributos(List<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, Map<String, ValorAtributoProducto>> resultado = new HashMap<>();
        for (ValorAtributoProducto valor : valorAtributoProductoRepository.findConAtributoPorProductoIds(ids)) {
            resultado.computeIfAbsent(valor.getProducto().getId(), ignored -> new HashMap<>())
                    .put(valor.getAtributo().getCodigo().toLowerCase(Locale.ROOT), valor);
        }
        return resultado;
    }

    private Set<UUID> idsSeleccionados(ValidarCambioRequestDto request) {
        Set<UUID> ids = new LinkedHashSet<>(Optional.ofNullable(request.getComponentesActuales()).orElse(List.of()));
        Stream.of(request.getCpuId(), request.getPlacaId(), request.getRamId(), request.getGpuId(), request.getFuenteId(),
                        request.getGabineteId(), request.getRefrigeracionId())
                .filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }

    private String texto(Map<String, ValorAtributoProducto> atributos, String codigo) {
        ValorAtributoProducto valor = atributos.get(codigo);
        return valor != null && valor.getValorTexto() != null && !valor.getValorTexto().isBlank()
                ? valor.getValorTexto().trim() : null;
    }

    private BigDecimal numero(Map<String, ValorAtributoProducto> atributos, String codigo) {
        ValorAtributoProducto valor = atributos.get(codigo);
        return valor != null ? valor.getValorNumero() : null;
    }

    private void compararMaximo(Map<String, ValorAtributoProducto> componente, String codigoValor,
                                Map<String, ValorAtributoProducto> limite, String codigoLimite,
                                String codigoRegla, String mensajeFallo, List<ResultadoRegla> resultados,
                                boolean condicionalSiSupera) {
        BigDecimal valor = numero(componente, codigoValor);
        BigDecimal maximo = numero(limite, codigoLimite);
        if (valor == null || maximo == null) {
            resultados.add(ResultadoRegla.porVerificar(codigoRegla,
                    "Faltan datos técnicos para verificar " + codigoRegla.toLowerCase(Locale.ROOT).replace('_', ' ') + "."));
        } else if (valor.compareTo(maximo) > 0) {
            resultados.add(condicionalSiSupera ? ResultadoRegla.condicional(codigoRegla, mensajeFallo)
                    : ResultadoRegla.incompatible(codigoRegla, mensajeFallo));
        } else {
            resultados.add(ResultadoRegla.verificada(codigoRegla, "La restricción " + codigoRegla + " se cumple."));
        }
    }

    private void validarOpcionIncluida(Map<String, ValorAtributoProducto> componente, String codigoValor,
                                       Map<String, ValorAtributoProducto> contenedor, String codigoOpciones,
                                       String codigoRegla, String mensajeFallo, List<ResultadoRegla> resultados) {
        String valor = texto(componente, codigoValor);
        String opciones = texto(contenedor, codigoOpciones);
        if (valor == null || opciones == null) {
            resultados.add(ResultadoRegla.porVerificar(codigoRegla, "Faltan datos técnicos para verificar " + codigoRegla + "."));
        } else if (!contieneOpcion(opciones, valor)) {
            resultados.add(ResultadoRegla.incompatible(codigoRegla, mensajeFallo));
        } else {
            resultados.add(ResultadoRegla.verificada(codigoRegla, "La restricción " + codigoRegla + " se cumple."));
        }
    }

    private boolean contieneOpcion(String opciones, String valor) {
        String objetivo = normalizarToken(valor);
        return Arrays.stream(Optional.ofNullable(opciones).orElse("").split("[,;/|\\n]+"))
                .map(this::normalizarToken)
                .anyMatch(opcion -> opcion.equals(objetivo) || opcion.startsWith(objetivo));
    }

    private boolean igualTecnico(String izquierda, String derecha) {
        return normalizarToken(izquierda).equals(normalizarToken(derecha));
    }

    private String normalizarToken(String valor) {
        return Optional.ofNullable(valor).orElse("").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private record ResultadoRegla(String codigo, String estado, String mensaje) {
        static ResultadoRegla verificada(String codigo, String mensaje) {
            return new ResultadoRegla(codigo, "VERIFICADA", mensaje);
        }
        static ResultadoRegla condicional(String codigo, String mensaje) {
            return new ResultadoRegla(codigo, "CONDICIONAL", mensaje);
        }
        static ResultadoRegla incompatible(String codigo, String mensaje) {
            return new ResultadoRegla(codigo, "INCOMPATIBLE", mensaje);
        }
        static ResultadoRegla porVerificar(String codigo, String mensaje) {
            return new ResultadoRegla(codigo, "POR_VERIFICAR", mensaje);
        }
    }

    @Override
    public CalcularConfiguracionResponseDto calcularConfiguracion(CalcularConfiguracionRequestDto request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Debe proporcionar al menos un componente en la configuración");
        }

        List<UUID> productIds = request.getItems().stream().map(ItemConfiguracionDto::getProductoId).toList();
        List<Producto> productos = productoRepository.findAllById(productIds);
        Map<UUID, Producto> productoMap = new HashMap<>();
        for (Producto p : productos) {
            productoMap.put(p.getId(), p);
        }

        // Stock disponible
        List<Object[]> stockRows = existenciaRepository.findStockDisponiblePorProductoIds(productIds);
        Map<UUID, Integer> stockMap = new HashMap<>();
        for (Object[] r : stockRows) {
            stockMap.put((UUID) r[0], ((Long) r[1]).intValue());
        }

        BigDecimal precioTotal = BigDecimal.ZERO;
        BigDecimal consumoTotalW = BigDecimal.ZERO;
        boolean todosDisponibles = true;
        boolean datosConsumoIncompletos = false;
        List<ItemCalculadoDto> itemsCalculados = new ArrayList<>();
        List<String> notas = new ArrayList<>();

        for (ItemConfiguracionDto itemReq : request.getItems()) {
            Producto prod = productoMap.get(itemReq.getProductoId());
            if (prod == null || Boolean.TRUE.equals(prod.getIsDeleted())) {
                throw new ResourceNotFoundException("El producto con ID " + itemReq.getProductoId() + " no existe o fue eliminado");
            }
            if (!Boolean.TRUE.equals(prod.getActivo()) || !Boolean.TRUE.equals(prod.getVendible())) {
                throw new BadRequestException("El producto " + prod.getNombre() + " (" + prod.getSku() + ") no se encuentra activo para la venta");
            }

            int stock = stockMap.getOrDefault(prod.getId(), 0);
            boolean suficienteStock = stock >= itemReq.getCantidad();
            if (!suficienteStock) {
                todosDisponibles = false;
                notas.add("Stock insuficiente para: " + prod.getNombre() + " (Solicitado: " + itemReq.getCantidad() + ", Disponible: " + stock + ")");
            }

            BigDecimal subtotal = prod.getPrecioBase().multiply(BigDecimal.valueOf(itemReq.getCantidad()));
            precioTotal = precioTotal.add(subtotal);

            // Obtener potencia estimada
            BigDecimal potenciaEstimada = BigDecimal.ZERO;
            Optional<EspecificacionCpu> cpuOpt = especificacionCpuRepository.findById(prod.getId());
            if (cpuOpt.isPresent()) {
                EspecificacionCpu cpu = cpuOpt.get();
                potenciaEstimada = cpu.getPotenciaMaximaW() != null ? cpu.getPotenciaMaximaW() : cpu.getPotenciaBaseW();
            } else {
                // Buscar atributos técnicos de consumo/potencia (TDP, Consumo W, etc.)
                List<ValorAtributoProducto> attrs = valorAtributoProductoRepository.findConAtributoPorProductoId(prod.getId());
                boolean encontrado = false;
                for (ValorAtributoProducto v : attrs) {
                    String cod = v.getAtributo().getCodigo().toLowerCase();
                    if ((cod.contains("tdp") || cod.contains("potencia") || cod.contains("consumo")) && v.getValorNumero() != null) {
                        potenciaEstimada = v.getValorNumero();
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    datosConsumoIncompletos = true;
                }
            }

            consumoTotalW = consumoTotalW.add(potenciaEstimada.multiply(BigDecimal.valueOf(itemReq.getCantidad())));

            itemsCalculados.add(ItemCalculadoDto.builder()
                    .productoId(prod.getId())
                    .nombre(prod.getNombre())
                    .sku(prod.getSku())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(prod.getPrecioBase())
                    .subtotal(subtotal)
                    .stockDisponible(stock)
                    .stockSuficiente(suficienteStock)
                    .potenciaEstimadaW(potenciaEstimada)
                    .build());
        }

        if (datosConsumoIncompletos) {
            notas.add("Advertencia: No todos los componentes poseen especificaciones de potencia/TDP en la base de datos. El consumo estimado no contempla los componentes sin ficha de potencia.");
        }

        // Recomendar fuente de poder
        Integer fuenteRecomendada = null;
        if (consumoTotalW.compareTo(BigDecimal.ZERO) > 0) {
            // Factor de seguridad 1.25x + margen base de 50W
            BigDecimal consumoConMargen = consumoTotalW.multiply(BigDecimal.valueOf(1.25)).add(BigDecimal.valueOf(50));
            int psuMin = consumoConMargen.setScale(0, RoundingMode.CEILING).intValue();
            fuenteRecomendada = calcularPsuEstandar(psuMin);
        }

        return CalcularConfiguracionResponseDto.builder()
                .items(itemsCalculados)
                .precioTotal(precioTotal)
                .consumoEstimadoW(consumoTotalW)
                .fuenteRecomendadaW(fuenteRecomendada)
                .todosDisponibles(todosDisponibles)
                .datosConsumoIncompletos(datosConsumoIncompletos)
                .notas(notas)
                .build();
    }

    private Integer calcularPsuEstandar(int w) {
        int[] estandares = {450, 500, 550, 600, 650, 700, 750, 850, 1000, 1200, 1600};
        for (int psu : estandares) {
            if (psu >= w) {
                return psu;
            }
        }
        return w;
    }

    private List<ProductoResumenDto> mapearProductosConStockEImagenes(List<Producto> productos) {
        if (productos.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = productos.stream().map(Producto::getId).toList();

        List<ImagenProducto> imagenes = imagenProductoRepository.findByProductoIdInAndIsDeletedFalseOrderByPosicionAsc(ids);
        Map<UUID, String> imagenMap = new HashMap<>();
        for (ImagenProducto img : imagenes) {
            UUID pId = img.getProducto().getId();
            if (!imagenMap.containsKey(pId) || Boolean.TRUE.equals(img.getPrincipal())) {
                imagenMap.put(pId, img.getUrl());
            }
        }

        List<Object[]> stockRows = existenciaRepository.findStockDisponiblePorProductoIds(ids);
        Map<UUID, Integer> stockMap = new HashMap<>();
        for (Object[] r : stockRows) {
            stockMap.put((UUID) r[0], ((Long) r[1]).intValue());
        }

        return productos.stream()
                .map(p -> productoMapper.toResumenDto(p, imagenMap.get(p.getId()), stockMap.getOrDefault(p.getId(), 0)))
                .toList();
    }
}
