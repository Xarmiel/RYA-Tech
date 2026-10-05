package com.ecommerce.backend.config;

import com.ecommerce.backend.model.Categoria;
import com.ecommerce.backend.model.EspecificacionTecnica;
import com.ecommerce.backend.model.Producto;
import com.ecommerce.backend.model.Rol;
import com.ecommerce.backend.model.Usuario;
import com.ecommerce.backend.model.enums.TipoProductoEnum;
import com.ecommerce.backend.repository.CategoriaRepository;
import com.ecommerce.backend.repository.ProductoRepository;
import com.ecommerce.backend.repository.RolRepository;
import com.ecommerce.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.data-seeder.enabled:false}")
    private boolean seederEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.info("Data Seeder está desactivado (app.data-seeder.enabled=false). Respetando datos en Supabase.");
            return;
        }
        log.info("Iniciando verificación y siembra de datos (Data Seeder)...");

        Map<String, Categoria> subcategoriasMap = inicializarCategorias();
        inicializarUsuarios();
        inicializarProductos(subcategoriasMap);

        log.info("Data Seeder completado con éxito. Total categorías: {}, Total productos: {}, Total usuarios: {}",
                categoriaRepository.count(), productoRepository.count(), usuarioRepository.count());
    }

    private Map<String, Categoria> inicializarCategorias() {
        Map<String, Categoria> subcategoriasMap = new HashMap<>();
        categoriaRepository.findAll().forEach(cat -> subcategoriasMap.put(cat.getNombre(), cat));

        Categoria catHardware = subcategoriasMap.get("Componente");
        if (catHardware == null) {
            catHardware = obtenerOCrearCategoria("Componente", "componente", null);
            subcategoriasMap.put("Componente", catHardware);
        }

        Categoria catPerifericos = subcategoriasMap.get("Periféricos");
        if (catPerifericos == null) {
            catPerifericos = obtenerOCrearCategoria(
                    "Periféricos", "perifericos", null);
            subcategoriasMap.put("Periféricos", catPerifericos);
        }

        String[] hardwareSubs = {
            "Placas Madre", "Procesadores", "Tarjetas Gráficas", "Almacenamiento", "Memoria RAM",
            "Fuentes de Poder", "Gabinetes", "Enfriamiento Líquido", "Ventiladores y Disipadores", "Tarjetas de Red"
        };

        String[] perifericosSubs = {
            "Monitores", "Teclados", "Ratones", "Auriculares", "Bocinas",
            "Cámaras Web", "Micrófonos USB", "Discos Duros Externos", "Hubs y Estaciones", "Mousepads"
        };

        for (String subNombre : hardwareSubs) {
            if (!subcategoriasMap.containsKey(subNombre)) {
                Categoria sub = obtenerOCrearCategoria(subNombre, toSlug(subNombre), catHardware);
                subcategoriasMap.put(subNombre, sub);
            }
        }

        for (String subNombre : perifericosSubs) {
            if (!subcategoriasMap.containsKey(subNombre)) {
                Categoria sub = obtenerOCrearCategoria(subNombre, toSlug(subNombre), catPerifericos);
                subcategoriasMap.put(subNombre, sub);
            }
        }

        return subcategoriasMap;
    }

    private Categoria obtenerOCrearCategoria(String nombre, String slug, Categoria parent) {
        return categoriaRepository.findBySlug(slug).orElseGet(() -> {
            Categoria categoria = new Categoria();
            categoria.setParent(parent);
            categoria.setNombre(nombre);
            categoria.setSlug(slug);
            return categoriaRepository.save(categoria);
        });
    }

    private void inicializarUsuarios() {
        Rol rolClient = rolRepository.findByNombre("CLIENT").orElseGet(() -> rolRepository.save(new Rol("CLIENT")));
        Rol rolAdmin = rolRepository.findByNombre("ADMIN").orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

        if (!usuarioRepository.findByEmail("demo@ryatech.pe").isPresent()) {
            log.info("Creando usuario de demostración (CLIENT)...");
            Usuario demoUser = new Usuario();
            demoUser.setNombre("Cliente Demo RYA");
            demoUser.setEmail("demo@ryatech.pe");
            demoUser.setTelefono("987654321");
            demoUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            demoUser.setRol(rolClient);
            usuarioRepository.save(demoUser);
        }

        if (!usuarioRepository.findByEmail("admin@ryatech.pe").isPresent()) {
            log.info("Creando usuario de administración (ADMIN)...");
            Usuario adminUser = new Usuario();
            adminUser.setNombre("Administrador RYA");
            adminUser.setEmail("admin@ryatech.pe");
            adminUser.setTelefono("999888777");
            adminUser.setPasswordHash(passwordEncoder.encode("Admin123!"));
            adminUser.setRol(rolAdmin);
            usuarioRepository.save(adminUser);
        }
    }

    private void inicializarProductos(Map<String, Categoria> subcategoriasMap) {
        if (productoRepository.count() >= 80) {
            log.info("El catálogo de productos ya está completo en la base de datos ({} productos).", productoRepository.count());
            return;
        }

        log.info("Poblando productos y especificaciones iniciales...");

        Set<String> skusExistentes = new HashSet<>(productoRepository.findAll().stream().map(Producto::getSku).toList());
        int counter = (int) productoRepository.count() + 1;
        List<Producto> productosAGuardar = new ArrayList<>();
        List<RawProduct> rawProducts = obtenerCatalogoCompleto();
        for (RawProduct raw : rawProducts) {
            String candidateSku = String.format("RYA-%04d", counter);
            if (skusExistentes.contains(candidateSku)) {
                counter++;
                continue;
            }

            Categoria subcat = subcategoriasMap.get(raw.categoria);
            if (subcat == null) {
                subcat = categoriaRepository.findByNombre(raw.categoria).orElse(null);
                if (subcat != null) {
                    subcategoriasMap.put(raw.categoria, subcat);
                }
            }
            if (subcat == null) {
                log.warn("Subcategoría no encontrada para: {}", raw.categoria);
                continue;
            }

            Producto prod = new Producto();
            prod.setCategoria(subcat);
            prod.setSku(String.format("RYA-%04d", counter++));
            prod.setNombre(raw.fabricante + " " + raw.categoria + " (" + raw.primerAtributo() + ")");
            prod.setDescripcion("Componente " + raw.fabricante + " categoría " + raw.categoria + " con alto rendimiento y garantía oficial.");
            prod.setPrecioBase(BigDecimal.valueOf(raw.precio));
            prod.setTipoProducto(esPeriferico(raw.categoria) ? TipoProductoEnum.PERIFERICO : TipoProductoEnum.HARDWARE);
            prod.setStock(25);
            prod.setActivo(true);

            List<EspecificacionTecnica> specs = new ArrayList<>();
            
            EspecificacionTecnica specFab = new EspecificacionTecnica();
            specFab.setProducto(prod);
            specFab.setClave("fabricante");
            specFab.setValor(raw.fabricante);
            specs.add(specFab);

            for (Map.Entry<String, String> entry : raw.atributos.entrySet()) {
                EspecificacionTecnica spec = new EspecificacionTecnica();
                spec.setProducto(prod);
                spec.setClave(entry.getKey());
                spec.setValor(entry.getValue());
                specs.add(spec);
            }

            prod.setEspecificaciones(specs);
            productosAGuardar.add(prod);
        }

        if (!productosAGuardar.isEmpty()) {
            productoRepository.saveAll(productosAGuardar);
            log.info("Se guardaron {} productos iniciales en la base de datos.", productosAGuardar.size());
        }
    }

    private boolean esPeriferico(String categoria) {
        return List.of("Monitores", "Teclados", "Ratones", "Auriculares", "Bocinas",
                "Cámaras Web", "Micrófonos USB", "Discos Duros Externos", "Hubs y Estaciones", "Mousepads")
                .contains(categoria);
    }

    private String toSlug(String input) {
        return input.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static class RawProduct {
        String categoria;
        String fabricante;
        double precio;
        Map<String, String> atributos;

        RawProduct(String categoria, String fabricante, double precio, Map<String, String> atributos) {
            this.categoria = categoria;
            this.fabricante = fabricante;
            this.precio = precio;
            this.atributos = atributos;
        }

        String primerAtributo() {
            return atributos.isEmpty() ? "Standard" : atributos.values().iterator().next();
        }
    }

    private List<RawProduct> obtenerCatalogoCompleto() {
        List<RawProduct> list = new ArrayList<>();

        list.add(new RawProduct("Placas Madre", "ASUS", 850, Map.of("socket_placa", "Para Intel LGA1700", "chipset_gama", "Gama Alta (Intel Z / AMD X)", "formato_placa", "ATX (Estándar)", "tipo_ram_placa", "DDR5", "conectividad_placa", "Con Wi-Fi y Bluetooth", "generacion_pcie_placa", "PCIe 5.0 (Ready)")));
        list.add(new RawProduct("Placas Madre", "MSI", 650, Map.of("socket_placa", "Para AMD AM5", "chipset_gama", "Gama Media (Intel B / AMD B)", "formato_placa", "Micro-ATX (Compacto)", "tipo_ram_placa", "DDR5", "conectividad_placa", "Solo Ethernet (Cable)", "generacion_pcie_placa", "PCIe 4.0")));
        list.add(new RawProduct("Placas Madre", "Gigabyte", 1200, Map.of("socket_placa", "Para Intel LGA1851", "chipset_gama", "Gama Alta (Intel Z / AMD X)", "formato_placa", "ATX (Estándar)", "tipo_ram_placa", "DDR5", "conectividad_placa", "Con Wi-Fi y Bluetooth", "generacion_pcie_placa", "PCIe 5.0 (Ready)")));
        list.add(new RawProduct("Placas Madre", "ASUS", 400, Map.of("socket_placa", "Para AMD AM4", "chipset_gama", "Gama Entrada (Intel H / AMD A)", "formato_placa", "Micro-ATX (Compacto)", "tipo_ram_placa", "DDR4", "conectividad_placa", "Solo Ethernet (Cable)", "generacion_pcie_placa", "PCIe 4.0")));
        list.add(new RawProduct("Placas Madre", "MSI", 950, Map.of("socket_placa", "Para Intel LGA1700", "chipset_gama", "Gama Media (Intel B / AMD B)", "formato_placa", "ATX (Estándar)", "tipo_ram_placa", "DDR5", "conectividad_placa", "Con Wi-Fi y Bluetooth", "generacion_pcie_placa", "PCIe 4.0")));

        list.add(new RawProduct("Procesadores", "Intel", 1850, Map.of("gama_cpu", "Gama Alta", "linea_cpu", "Intel Core i9 / AMD Ryzen 9", "socket_cpu", "Intel LGA1700", "tecnologia_cpu", "Desbloqueado (Overclock)", "presupuesto_cpu", "Rango de precio")));
        list.add(new RawProduct("Procesadores", "AMD", 2100, Map.of("gama_cpu", "Gama Entusiasta", "linea_cpu", "Intel Core i9 / AMD Ryzen 9", "socket_cpu", "AMD AM5", "tecnologia_cpu", "Con Caché 3D (AMD X3D)", "presupuesto_cpu", "Rango de precio")));
        list.add(new RawProduct("Procesadores", "Intel", 1250, Map.of("gama_cpu", "Gama Media", "linea_cpu", "Intel Core i7 / AMD Ryzen 7", "socket_cpu", "Intel LGA1700", "tecnologia_cpu", "Desbloqueado (Overclock)", "presupuesto_cpu", "Rango de precio")));
        list.add(new RawProduct("Procesadores", "AMD", 1650, Map.of("gama_cpu", "Gama Alta", "linea_cpu", "Intel Core i7 / AMD Ryzen 7", "socket_cpu", "AMD AM5", "tecnologia_cpu", "Con Caché 3D (AMD X3D)", "presupuesto_cpu", "En Oferta")));
        list.add(new RawProduct("Procesadores", "Intel", 950, Map.of("gama_cpu", "Gama Media", "linea_cpu", "Intel Core i5 / AMD Ryzen 5", "socket_cpu", "Intel LGA1700", "tecnologia_cpu", "Requiere Gráfica Dedicada", "presupuesto_cpu", "En Combo (CPU + Placa)")));

        list.add(new RawProduct("Tarjetas Gráficas", "ASUS", 2900, Map.of("chipset_gpu", "NVIDIA GeForce", "gama_resolucion", "4K Ultra / VR", "memoria_vram", "16 GB", "serie_gpu", "NVIDIA RTX Serie 50", "ensamblador_gpu", "ASUS", "enfriamiento_gpu", "3 Ventiladores (Gama Alta / Grande)", "conector_energia_gpu", "Conector Nuevo")));
        list.add(new RawProduct("Tarjetas Gráficas", "MSI", 1550, Map.of("chipset_gpu", "AMD Radeon", "gama_resolucion", "1440p Competitivo / 4K", "memoria_vram", "12 GB", "serie_gpu", "AMD Radeon RX Serie 7000", "ensamblador_gpu", "MSI", "enfriamiento_gpu", "2 Ventiladores (Estándar)", "conector_energia_gpu", "Estándar PCIe")));
        list.add(new RawProduct("Tarjetas Gráficas", "EVGA", 780, Map.of("chipset_gpu", "NVIDIA GeForce", "gama_resolucion", "1080p Ultra / 1440p", "memoria_vram", "8 GB", "serie_gpu", "NVIDIA RTX Serie 40", "ensamblador_gpu", "EVGA / Zotac / PNY", "enfriamiento_gpu", "2 Ventiladores (Estándar)", "conector_energia_gpu", "Estándar PCIe")));
        list.add(new RawProduct("Tarjetas Gráficas", "Gigabyte", 620, Map.of("chipset_gpu", "NVIDIA GeForce", "gama_resolucion", "Competitivo / 1080p", "memoria_vram", "6 GB o menos", "serie_gpu", "NVIDIA RTX Serie 40", "ensamblador_gpu", "Gigabyte", "enfriamiento_gpu", "1 Ventilador (ITX / Compacto)", "conector_energia_gpu", "Estándar PCIe")));
        list.add(new RawProduct("Tarjetas Gráficas", "Sapphire", 3400, Map.of("chipset_gpu", "AMD Radeon", "gama_resolucion", "4K Ultra / VR", "memoria_vram", "24 GB o más", "serie_gpu", "AMD Radeon RX Serie 7000", "ensamblador_gpu", "ASUS", "enfriamiento_gpu", "Enfriamiento Líquido (AIO)", "conector_energia_gpu", "Conector Nuevo")));

        list.add(new RawProduct("Almacenamiento", "Samsung", 850, Map.of("tipo_almacenamiento", "M.2 NVMe", "capacidad_disco", "1 TB", "interfaz_disco", "PCIe Gen 4.0", "velocidad_lectura_disco", "Hasta 7,500 MB/s", "formato_disco", "M.2 2280", "caracteristicas_disco", "Con Disipador de Calor Integrado")));
        list.add(new RawProduct("Almacenamiento", "Western Digital", 1200, Map.of("tipo_almacenamiento", "M.2 NVMe", "capacidad_disco", "2 TB", "interfaz_disco", "PCIe Gen 5.0", "velocidad_lectura_disco", "Más de 10,000 MB/s", "formato_disco", "M.2 2280", "caracteristicas_disco", "Compatible con PlayStation 5")));
        list.add(new RawProduct("Almacenamiento", "Crucial", 350, Map.of("tipo_almacenamiento", "SSD SATA", "capacidad_disco", "500 GB o menos", "interfaz_disco", "SATA III", "velocidad_lectura_disco", "Hasta 3,500 MB/s", "formato_disco", "2.5 Pulgadas", "caracteristicas_disco", "Ninguna")));
        list.add(new RawProduct("Almacenamiento", "Seagate", 280, Map.of("tipo_almacenamiento", "HDD Mecánico", "capacidad_disco", "4 TB o más", "interfaz_disco", "SATA III", "velocidad_lectura_disco", "Hasta 3,500 MB/s", "formato_disco", "3.5 Pulgadas", "caracteristicas_disco", "Ninguna")));
        list.add(new RawProduct("Almacenamiento", "Kingston", 650, Map.of("tipo_almacenamiento", "M.2 NVMe", "capacidad_disco", "1 TB", "interfaz_disco", "PCIe Gen 4.0", "velocidad_lectura_disco", "Hasta 7,500 MB/s", "formato_disco", "M.2 2280", "caracteristicas_disco", "Con Disipador de Calor Integrado")));

        list.add(new RawProduct("Memoria RAM", "Kingston", 480, Map.of("generacion_ram", "DDR5", "capacidad_ram", "32 GB", "configuracion_ram", "2 Módulos (Dual Channel)", "velocidad_ram", "5200 MHz a 6000 MHz (Estándar DDR5)", "estetica_ram", "Con Iluminación RGB", "perfiles_ram", "Compatible con Intel XMP")));
        list.add(new RawProduct("Memoria RAM", "Corsair", 350, Map.of("generacion_ram", "DDR5", "capacidad_ram", "16 GB", "configuracion_ram", "2 Módulos (Dual Channel)", "velocidad_ram", "5200 MHz a 6000 MHz (Estándar DDR5)", "estetica_ram", "Sin RGB (Diseño discreto / Negro)", "perfiles_ram", "Compatible con AMD EXPO")));
        list.add(new RawProduct("Memoria RAM", "G.Skill", 680, Map.of("generacion_ram", "DDR5", "capacidad_ram", "64 GB", "configuracion_ram", "4 Módulos (Quad Channel)", "velocidad_ram", "Más de 6400 MHz (Gama Alta DDR5)", "estetica_ram", "Con Iluminación RGB", "perfiles_ram", "Compatible con Intel XMP")));
        list.add(new RawProduct("Memoria RAM", "Crucial", 210, Map.of("generacion_ram", "DDR4", "capacidad_ram", "16 GB", "configuracion_ram", "1 Módulo (Single Channel)", "velocidad_ram", "Hasta 3200 MHz (Estándar DDR4)", "estetica_ram", "Sin RGB (Diseño discreto / Negro)", "perfiles_ram", "Ninguno")));
        list.add(new RawProduct("Memoria RAM", "Kingston", 280, Map.of("generacion_ram", "DDR4", "capacidad_ram", "32 GB", "configuracion_ram", "2 Módulos (Dual Channel)", "velocidad_ram", "3600 MHz a 4000 MHz (Alto rendimiento DDR4)", "estetica_ram", "Perfil Bajo", "perfiles_ram", "Ninguno")));

        list.add(new RawProduct("Fuentes de Poder", "Corsair", 450, Map.of("potencia_psu", "850W a 1000W (Gama Alta)", "certificacion_psu", "80 Plus Gold", "modularidad_psu", "Full Modular (Cables 100% desmontables)", "estandar_psu", "ATX 3.0 / 3.1 PCIe 5.0 Ready", "formato_psu", "ATX (Estándar para gabinetes normales)", "estetica_psu", "Color Negro")));
        list.add(new RawProduct("Fuentes de Poder", "EVGA", 320, Map.of("potencia_psu", "650W a 750W (Gama Media)", "certificacion_psu", "80 Plus Bronze", "modularidad_psu", "Semimodular (Solo cables básicos fijos)", "estandar_psu", "ATX 2.0 tradicional", "formato_psu", "ATX (Estándar para gabinetes normales)", "estetica_psu", "Color Negro")));
        list.add(new RawProduct("Fuentes de Poder", "Thermaltake", 580, Map.of("potencia_psu", "850W a 1000W (Gama Alta)", "certificacion_psu", "80 Plus Platinum", "modularidad_psu", "Full Modular (Cables 100% desmontables)", "estandar_psu", "ATX 3.0 / 3.1 PCIe 5.0 Ready", "formato_psu", "ATX (Estándar para gabinetes normales)", "estetica_psu", "Color Blanco")));
        list.add(new RawProduct("Fuentes de Poder", "Cooler Master", 200, Map.of("potencia_psu", "550W o menos (Gama Entrada)", "certificacion_psu", "80 Plus White", "modularidad_psu", "No Modular (Todos los cables fijos)", "estandar_psu", "ATX 2.0 tradicional", "formato_psu", "ATX (Estándar para gabinetes normales)", "estetica_psu", "Color Negro")));
        list.add(new RawProduct("Fuentes de Poder", "Corsair", 780, Map.of("potencia_psu", "Más de 1200W (Gama Entusiasta)", "certificacion_psu", "80 Plus Titanium", "modularidad_psu", "Full Modular (Cables 100% desmontables)", "estandar_psu", "ATX 3.0 / 3.1 PCIe 5.0 Ready", "formato_psu", "ATX (Estándar para gabinetes normales)", "estetica_psu", "Color Blanco")));

        list.add(new RawProduct("Gabinetes", "NZXT", 450, Map.of("tamaño_gabinete", "Mid Tower", "compatibilidad_aio_gabinete", "Soporta hasta 360mm / 420mm", "diseno_gabinete", "Frente de Malla", "ventiladores_incluidos", "3 a 4 ventiladores", "iluminacion_gabinete", "Sin Iluminación", "color_gabinete", "Negro")));
        list.add(new RawProduct("Gabinetes", "Corsair", 380, Map.of("tamaño_gabinete", "Mid Tower", "compatibilidad_aio_gabinete", "Soporta hasta 240mm / 280mm", "diseno_gabinete", "Con Vidrio Templado Lateral", "ventiladores_incluidos", "1 a 2 ventiladores", "iluminacion_gabinete", "Con Ventiladores ARGB / RGB", "color_gabinete", "Blanco")));
        list.add(new RawProduct("Gabinetes", "Lian Li", 650, Map.of("tamaño_gabinete", "Full Tower", "compatibilidad_aio_gabinete", "Soporta hasta 360mm / 420mm", "diseno_gabinete", "Tipo Pecera", "ventiladores_incluidos", "Más de 4 ventiladores", "iluminacion_gabinete", "Con Ventiladores ARGB / RGB", "color_gabinete", "Negro")));
        list.add(new RawProduct("Gabinetes", "Cooler Master", 280, Map.of("tamaño_gabinete", "Micro-ATX Tower", "compatibilidad_aio_gabinete", "Soporta hasta 120mm / 140mm", "diseno_gabinete", "Estilo Minimalista / Cerrado", "ventiladores_incluidos", "Sin ventiladores", "iluminacion_gabinete", "Sin Iluminación", "color_gabinete", "Negro")));
        list.add(new RawProduct("Gabinetes", "Thermaltake", 520, Map.of("tamaño_gabinete", "Mid Tower", "compatibilidad_aio_gabinete", "Soporta hasta 280mm", "diseno_gabinete", "Frente de Malla", "ventiladores_incluidos", "3 a 4 ventiladores", "iluminacion_gabinete", "Con Ventiladores ARGB / RGB", "color_gabinete", "Blanco")));

        list.add(new RawProduct("Enfriamiento Líquido", "Corsair", 350, Map.of("tamaño_radiador", "360 mm (3 ventiladores de 120mm)", "socket_aio", "Intel LGA1700 / LGA1851", "pantalla_aio", "Con Pantalla LCD personalizable", "estetica_aio", "Ventiladores ARGB", "color_aio", "Completamente Negro", "conectividad_aio", "Conexión por Cable Único")));
        list.add(new RawProduct("Enfriamiento Líquido", "NZXT", 420, Map.of("tamaño_radiador", "280 mm (2 ventiladores de 140mm)", "socket_aio", "AMD AM5", "pantalla_aio", "Con Pantalla Digital básica", "estetica_aio", "Ventiladores ARGB", "color_aio", "Completamente Negro", "conectividad_aio", "Conexión Tradicional")));
        list.add(new RawProduct("Enfriamiento Líquido", "Cooler Master", 250, Map.of("tamaño_radiador", "240 mm (2 ventiladores de 120mm)", "socket_aio", "Intel LGA1200 / 115X", "pantalla_aio", "Sin Pantalla", "estetica_aio", "Ventiladores RGB Estáticos", "color_aio", "Completamente Negro", "conectividad_aio", "Conexión Tradicional")));
        list.add(new RawProduct("Enfriamiento Líquido", "Thermaltake", 380, Map.of("tamaño_radiador", "360 mm (3 ventiladores de 120mm)", "socket_aio", "AMD AM4", "pantalla_aio", "Sin Pantalla", "estetica_aio", "Ventiladores ARGB", "color_aio", "Completamente Blanco", "conectividad_aio", "Conexión por Cable Único")));
        list.add(new RawProduct("Enfriamiento Líquido", "ASUS", 580, Map.of("tamaño_radiador", "420 mm (3 ventiladores de 140mm)", "socket_aio", "Intel LGA1851", "pantalla_aio", "Con Pantalla LCD personalizable", "estetica_aio", "Ventiladores ARGB", "color_aio", "Completamente Negro", "conectividad_aio", "Conexión por Cable Único")));

        list.add(new RawProduct("Ventiladores y Disipadores", "Corsair", 90, Map.of("tamaño_ventilador", "120 mm", "iluminacion_fan", "ARGB", "formato_venta_fan", "Kit de 3 Ventiladores", "conexion_fan", "PWM de 4 pines", "color_fan", "Negro")));
        list.add(new RawProduct("Ventiladores y Disipadores", "Noctua", 120, Map.of("tamaño_ventilador", "140 mm", "iluminacion_fan", "Sin LED", "formato_venta_fan", "Ventilador Individual", "conexion_fan", "PWM de 4 pines", "color_fan", "Negro")));
        list.add(new RawProduct("Ventiladores y Disipadores", "Cooler Master", 70, Map.of("tamaño_ventilador", "120 mm", "iluminacion_fan", "RGB Fijo / Monocromático", "formato_venta_fan", "Kit de 2 Ventiladores", "conexion_fan", "Molex / 3 pines", "color_fan", "Blanco")));
        list.add(new RawProduct("Ventiladores y Disipadores", "Lian Li", 150, Map.of("tamaño_ventilador", "140 mm", "iluminacion_fan", "ARGB", "formato_venta_fan", "Kit de 3 Ventiladores", "conexion_fan", "Conexión Magnética / Cadena", "color_fan", "Blanco")));
        list.add(new RawProduct("Ventiladores y Disipadores", "Thermaltake", 60, Map.of("tamaño_ventilador", "120 mm", "iluminacion_fan", "Sin LED", "formato_venta_fan", "Ventilador Individual", "conexion_fan", "PWM de 4 pines", "color_fan", "Negro")));

        list.add(new RawProduct("Tarjetas de Red", "ASUS", 180, Map.of("interfaz_red", "PCIe", "conectividad_red", "Wi-Fi + Bluetooth", "generacion_wifi", "Wi-Fi 6E / 6", "velocidad_lan", "2.5 Gbps", "antenas_red", "Con Antena Externa Base", "perfil_red", "Perfil Alto")));
        list.add(new RawProduct("Tarjetas de Red", "TP-Link", 120, Map.of("interfaz_red", "USB", "conectividad_red", "Wi-Fi + Bluetooth", "generacion_wifi", "Wi-Fi 7", "velocidad_lan", "1 Gbps", "antenas_red", "Con Antenas Directas", "perfil_red", "Perfil Alto")));
        list.add(new RawProduct("Tarjetas de Red", "Intel", 250, Map.of("interfaz_red", "PCIe", "conectividad_red", "Solo Wi-Fi", "generacion_wifi", "Wi-Fi 7", "velocidad_lan", "10 Gbps", "antenas_red", "Con Antena Externa Base", "perfil_red", "Perfil Bajo")));
        list.add(new RawProduct("Tarjetas de Red", "MSI", 80, Map.of("interfaz_red", "M.2 Key E", "conectividad_red", "Solo Bluetooth", "generacion_wifi", "Wi-Fi 5", "velocidad_lan", "1 Gbps", "antenas_red", "Nano / Sin Antena Externa", "perfil_red", "Perfil Bajo")));
        list.add(new RawProduct("Tarjetas de Red", "Gigabyte", 140, Map.of("interfaz_red", "PCIe", "conectividad_red", "Ethernet", "generacion_wifi", "Wi-Fi 6E / 6", "velocidad_lan", "2.5 Gbps", "antenas_red", "Con Antenas Directas", "perfil_red", "Perfil Alto")));

        list.add(new RawProduct("Monitores", "LG", 1150, Map.of("tamaño_monitor", "27 pulgadas", "resolucion_monitor", "Quad HD (1440p / 2K)", "tasa_refresco", "165Hz - 180Hz", "panel_monitor", "IPS (Colores precisos)", "forma_monitor", "Plana", "sincronizacion_monitor", "AMD FreeSync (Premium)")));
        list.add(new RawProduct("Monitores", "Samsung", 750, Map.of("tamaño_monitor", "24 pulgadas o menos", "resolucion_monitor", "Full HD (1080p)", "tasa_refresco", "165Hz - 180Hz", "panel_monitor", "VA (Mejor contraste)", "forma_monitor", "Curva", "sincronizacion_monitor", "AMD FreeSync (Premium)")));
        list.add(new RawProduct("Monitores", "ASUS", 1800, Map.of("tamaño_monitor", "32 pulgadas", "resolucion_monitor", "4K Ultra HD", "tasa_refresco", "100Hz - 144Hz", "panel_monitor", "IPS (Colores precisos)", "forma_monitor", "Plana", "sincronizacion_monitor", "NVIDIA G-Sync (Compatible)")));
        list.add(new RawProduct("Monitores", "MSI", 1250, Map.of("tamaño_monitor", "27 pulgadas", "resolucion_monitor", "Quad HD (1440p / 2K)", "tasa_refresco", "240Hz", "panel_monitor", "VA (Mejor contraste)", "forma_monitor", "Curva", "sincronizacion_monitor", "AMD FreeSync (Premium)")));
        list.add(new RawProduct("Monitores", "Gigabyte", 1450, Map.of("tamaño_monitor", "27 pulgadas", "resolucion_monitor", "Quad HD (1440p / 2K)", "tasa_refresco", "165Hz - 180Hz", "panel_monitor", "IPS (Colores precisos)", "forma_monitor", "Plana", "sincronizacion_monitor", "NVIDIA G-Sync (Compatible)")));

        list.add(new RawProduct("Teclados", "Logitech", 330, Map.of("tipo_teclado", "Mecánico", "formato_teclado", "Completo (100% / Con teclado numérico)", "tipo_switch", "Switch Brown (Táctil / Intermedio)", "conectividad_teclado", "Inalámbrico 2.4 GHz (Dongle USB / Sin retraso)", "iluminacion_teclado", "RGB Zona / Fijo", "idioma_teclado", "Español (Con tecla Ñ)")));
        list.add(new RawProduct("Teclados", "Razer", 480, Map.of("tipo_teclado", "Mecánico", "formato_teclado", "TKL (80% / Sin teclado numérico)", "tipo_switch", "Switch Green", "conectividad_teclado", "Alámbrico (Cable USB)", "iluminacion_teclado", "ARGB / RGB Per-Key (Tecla por tecla)", "idioma_teclado", "Inglés (US Layout)")));
        list.add(new RawProduct("Teclados", "Corsair", 550, Map.of("tipo_teclado", "Óptico", "formato_teclado", "Completo (100% / Con teclado numérico)", "tipo_switch", "Switch Red (Lineal / Silencioso)", "conectividad_teclado", "Alámbrico (Cable USB)", "iluminacion_teclado", "ARGB / RGB Per-Key (Tecla por tecla)", "idioma_teclado", "Español (Con tecla Ñ)")));
        list.add(new RawProduct("Teclados", "HyperX", 290, Map.of("tipo_teclado", "Semi-mecánico", "formato_teclado", "Completo (100% / Con teclado numérico)", "tipo_switch", "Switch Red (Lineal / Silencioso)", "conectividad_teclado", "Inalámbrico 2.4 GHz (Dongle USB / Sin retraso)", "iluminacion_teclado", "RGB Zona / Fijo", "idioma_teclado", "Español (Con tecla Ñ)")));
        list.add(new RawProduct("Teclados", "SteelSeries", 410, Map.of("tipo_teclado", "Mecánico", "formato_teclado", "75% / 65% (Compacto con flechas)", "tipo_switch", "Switch Brown (Táctil / Intermedio)", "conectividad_teclado", "Bluetooth (Multidispositivo)", "iluminacion_teclado", "ARGB / RGB Per-Key (Tecla por tecla)", "idioma_teclado", "Inglés (US Layout)")));

        list.add(new RawProduct("Ratones", "Logitech", 450, Map.of("conectividad_mouse", "Inalámbrico (2.4 GHz sin retraso)", "peso_mouse", "Ligero (60g a 80g)", "botones_mouse", "Estándar (5 a 6 botones)", "sensor_mouse", "Óptico de Alta Precisión", "diseno_mouse", "Para Diestros", "iluminacion_mouse", "Sin Iluminación")));
        list.add(new RawProduct("Ratones", "Razer", 380, Map.of("conectividad_mouse", "Inalámbrico (2.4 GHz sin retraso)", "peso_mouse", "Ultra Ligero (Menos de 60g)", "botones_mouse", "Estándar (5 a 6 botones)", "sensor_mouse", "Óptico de Alta Precisión", "diseno_mouse", "Ambidextro / Simétrico", "iluminacion_mouse", "RGB Direccionable (ARGB)")));
        list.add(new RawProduct("Ratones", "SteelSeries", 320, Map.of("conectividad_mouse", "Alámbrico (Cable USB)", "peso_mouse", "Estándar / Pesado (Más de 80g)", "botones_mouse", "Multi-botón / MOBA-MMO (9 a 12+ botones)", "sensor_mouse", "Láser", "diseno_mouse", "Ergonómico Vertical", "iluminacion_mouse", "RGB Fijo")));
        list.add(new RawProduct("Ratones", "Corsair", 290, Map.of("conectividad_mouse", "Alámbrico (Cable USB)", "peso_mouse", "Ligero (60g a 80g)", "botones_mouse", "Estándar (5 a 6 botones)", "sensor_mouse", "Óptico de Alta Precisión", "diseno_mouse", "Para Diestros", "iluminacion_mouse", "RGB Direccionable (ARGB)")));
        list.add(new RawProduct("Ratones", "Logitech", 620, Map.of("conectividad_mouse", "Bluetooth (Oficina / Multidispositivo)", "peso_mouse", "Ultra Ligero (Menos de 60g)", "botones_mouse", "Estándar (5 a 6 botones)", "sensor_mouse", "Óptico de Alta Precisión", "diseno_mouse", "Ambidextro / Simétrico", "iluminacion_mouse", "Sin Iluminación")));

        list.add(new RawProduct("Auriculares", "Logitech", 280, Map.of("conectividad_headset", "Inalámbrico (2.4 GHz sin retraso)", "formato_headset", "Over-Ear (Circumaural / Cubre toda la oreja)", "sonido_headset", "Sonido Envolvente Virtual (7.1 Surround)", "microfono_headset", "Micrófono Retráctil / Abatible", "plataforma_headset", "PC / Mac", "iluminacion_headset", "Con Iluminación RGB")));
        list.add(new RawProduct("Auriculares", "Razer", 350, Map.of("conectividad_headset", "Alámbrico (Jack 3.5mm / USB)", "formato_headset", "Over-Ear (Circumaural / Cubre toda la oreja)", "sonido_headset", "Sonido Estéreo (2.0)", "microfono_headset", "Con Cancelación de Ruido (ENC)", "plataforma_headset", "PlayStation (PS4 / PS5)", "iluminacion_headset", "Con Iluminación RGB")));
        list.add(new RawProduct("Auriculares", "HyperX", 220, Map.of("conectividad_headset", "Alámbrico (Jack 3.5mm / USB)", "formato_headset", "Over-Ear (Circumaural / Cubre toda la oreja)", "sonido_headset", "Sonido Estéreo (2.0)", "microfono_headset", "Micrófono Desmontable", "plataforma_headset", "Xbox (One / Series X|S)", "iluminacion_headset", "Sin Iluminación")));
        list.add(new RawProduct("Auriculares", "SteelSeries", 420, Map.of("conectividad_headset", "Bluetooth (Consolas portátiles / Celular)", "formato_headset", "On-Ear (Supraural / Se apoya en la oreja)", "sonido_headset", "Sonido Envolvente Virtual (7.1 Surround)", "microfono_headset", "Micrófono Retráctil / Abatible", "plataforma_headset", "Nintendo Switch", "iluminacion_headset", "Ediciones Especiales")));
        list.add(new RawProduct("Auriculares", "Corsair", 310, Map.of("conectividad_headset", "Inalámbrico (2.4 GHz sin retraso)", "formato_headset", "Over-Ear (Circumaural / Cubre toda la oreja)", "sonido_headset", "Sonido Envolvente Virtual (7.1 Surround)", "microfono_headset", "Con Cancelación de Ruido (ENC)", "plataforma_headset", "PC / Mac", "iluminacion_headset", "Con Iluminación RGB")));

        list.add(new RawProduct("Bocinas", "Logitech", 180, Map.of("tipo_audio_bocina", "Bocinas Estándar (Sistema 2.0 / 2.1)", "conectividad_bocina", "Alámbrico (Auxiliar 3.5mm / USB / Óptico)", "subwoofer_bocina", "Con Subwoofer Externo (Inalámbrico / Alámbrico)", "alimentacion_bocina", "Corriente Alterna (Enchufe de pared)", "iluminacion_bocina", "Con Iluminación RGB")));
        list.add(new RawProduct("Bocinas", "Razer", 250, Map.of("tipo_audio_bocina", "Barra de Sonido (Diseño Horizontal)", "conectividad_bocina", "Inalámbrico (Bluetooth)", "subwoofer_bocina", "Con Subwoofer Integrado", "alimentacion_bocina", "Por USB", "iluminacion_bocina", "Sin Iluminación")));
        list.add(new RawProduct("Bocinas", "Creative", 380, Map.of("tipo_audio_bocina", "Sonido Envolvente (Sistema 5.1 o superior)", "conectividad_bocina", "Alámbrico (Auxiliar 3.5mm / USB / Óptico)", "subwoofer_bocina", "Con Subwoofer Externo (Inalámbrico / Alámbrico)", "alimentacion_bocina", "Corriente Alterna (Enchufe de pared)", "iluminacion_bocina", "Con Iluminación RGB")));
        list.add(new RawProduct("Bocinas", "Samsung", 450, Map.of("tipo_audio_bocina", "Barra de Sonido (Diseño Horizontal)", "conectividad_bocina", "Inalámbrico (Bluetooth)", "subwoofer_bocina", "Con Subwoofer Externo (Inalámbrico / Alámbrico)", "alimentacion_bocina", "Batería Recargable (Portátil)", "iluminacion_bocina", "Sin Iluminación")));
        list.add(new RawProduct("Bocinas", "JBL", 120, Map.of("tipo_audio_bocina", "Bocinas Estándar (Sistema 2.0 / 2.1)", "conectividad_bocina", "Inalámbrico (Bluetooth)", "subwoofer_bocina", "Sin Subwoofer", "alimentacion_bocina", "Batería Recargable (Portátil)", "iluminacion_bocina", "Sin Iluminación")));

        list.add(new RawProduct("Cámaras Web", "Logitech", 380, Map.of("resolucion_webcam", "4K Ultra HD (Streaming profesional)", "fps_webcam", "60 FPS o más", "enfoque_webcam", "Enfoque Automático", "iluminacion_webcam", "Sin Iluminación", "privacidad_webcam", "Con Cubierta de Privacidad", "conectividad_webcam", "Cable USB-C")));
        list.add(new RawProduct("Cámaras Web", "Razer", 280, Map.of("resolucion_webcam", "2K / 1440p (Alta definición)", "fps_webcam", "60 FPS o más", "enfoque_webcam", "Enfoque Automático", "iluminacion_webcam", "Con Aro de Luz LED", "privacidad_webcam", "Con Cubierta de Privacidad", "conectividad_webcam", "Cable USB-A")));
        list.add(new RawProduct("Cámaras Web", "Microsoft", 150, Map.of("resolucion_webcam", "Full HD (1080p / Estándar)", "fps_webcam", "30 FPS", "enfoque_webcam", "Enfoque Fijo", "iluminacion_webcam", "Sin Iluminación", "privacidad_webcam", "Compatible con Windows Hello", "conectividad_webcam", "Cable USB-A")));
        list.add(new RawProduct("Cámaras Web", "ASUS", 320, Map.of("resolucion_webcam", "4K Ultra HD (Streaming profesional)", "fps_webcam", "60 FPS o más", "enfoque_webcam", "Enfoque Automático", "iluminacion_webcam", "Con Aro de Luz LED", "privacidad_webcam", "Con Cubierta de Privacidad", "conectividad_webcam", "Cable USB-C")));
        list.add(new RawProduct("Cámaras Web", "Creative", 90, Map.of("resolucion_webcam", "HD (720p / Económico)", "fps_webcam", "30 FPS", "enfoque_webcam", "Enfoque Fijo", "iluminacion_webcam", "Sin Iluminación", "privacidad_webcam", "Con Cubierta de Privacidad", "conectividad_webcam", "Cable USB-A")));

        return list;
    }
}
