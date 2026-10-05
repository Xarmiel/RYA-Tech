const REGLAS_DE_DEPENDENCIA = {
  "Placas Madre": { activa: ["socket_placa", "chipset_gama", "formato_placa", "tipo_ram_placa", "conectividad_placa", "generacion_pcie_placa"] },
  "Procesadores": { activa: ["gama_cpu", "linea_cpu", "socket_cpu", "tecnologia_cpu", "presupuesto_cpu"] },
  "Tarjetas Gráficas": { activa: ["chipset_gpu", "gama_resolucion", "memoria_vram", "serie_gpu", "ensamblador_gpu", "enfriamiento_gpu", "conector_energia_gpu"] },
  "Almacenamiento": { activa: ["tipo_almacenamiento", "capacidad_disco", "interfaz_disco", "velocidad_lectura_disco", "formato_disco", "caracteristicas_disco"] },
  "Memoria RAM": { activa: ["generacion_ram", "capacidad_ram", "configuracion_ram", "velocidad_ram", "estetica_ram", "perfiles_ram"] },
  "Fuentes de Poder": { activa: ["potencia_psu", "certificacion_psu", "modularidad_psu", "estandar_psu", "formato_psu", "estetica_psu"] },
  "Gabinetes": { activa: ["tamaño_gabinete", "compatibilidad_aio_gabinete", "diseno_gabinete", "ventiladores_incluidos", "iluminacion_gabinete", "color_gabinete"] },
  "Enfriamiento Líquido": { activa: ["tamaño_radiador", "socket_aio", "pantalla_aio", "estetica_aio", "color_aio", "conectividad_aio"] },
  "Ventiladores y Disipadores": { activa: ["tamaño_ventilador", "iluminacion_fan", "formato_venta_fan", "conexion_fan", "color_fan"] },
  "Tarjetas de Red": { activa: ["interfaz_red", "conectividad_red", "generacion_wifi", "velocidad_lan", "antenas_red", "perfil_red"] },
  "Monitores": { activa: ["tamaño_monitor", "resolucion_monitor", "tasa_refresco", "panel_monitor", "forma_monitor", "sincronizacion_monitor"] },
  "Teclados": { activa: ["tipo_teclado", "formato_teclado", "tipo_switch", "conectividad_teclado", "iluminacion_teclado", "idioma_teclado"] },
  "Ratones": { activa: ["conectividad_mouse", "peso_mouse", "botones_mouse", "sensor_mouse", "diseno_mouse", "iluminacion_mouse"] },
  "Auriculares": { activa: ["conectividad_headset", "formato_headset", "sonido_headset", "microfono_headset", "plataforma_headset", "iluminacion_headset"] },
  "Bocinas": { activa: ["tipo_audio_bocina", "conectividad_bocina", "subwoofer_bocina", "alimentacion_bocina", "iluminacion_bocina"] },
  "Cámaras Web": { activa: ["resolucion_webcam", "fps_webcam", "enfoque_webcam", "iluminacion_webcam", "privacidad_webcam", "conectividad_webcam"] },
  "Micrófonos USB": { activa: ["patron_polar_mic", "calidad_audio_mic", "controles_mic", "accesorios_mic", "iluminacion_mic"] },
  "Discos Duros Externos": { activa: ["tipo_disco_externo", "capacidad_externo", "formato_externo", "conectividad_externo", "consola_externo", "proteccion_externo"] },
  "Hubs y Estaciones": { activa: ["tipo_hub", "puertos_entrada_hub", "puertos_salida_hub", "energia_hub", "alimentacion_hub", "mandos_hub"] },
  "Mousepads": { activa: ["tamaño_mousepad", "superficie_mousepad", "material_mousepad", "iluminacion_mousepad", "grosor_mousepad", "caracteristicas_mousepad"] }
};

const FILTROS_GLOBALES = ["fabricante"];

const ATRIBUTOS_TARJETA = {
  "Placas Madre": ["socket_placa", "chipset_gama", "formato_placa"],
  "Procesadores": ["linea_cpu", "socket_cpu"],
  "Tarjetas Gráficas": ["serie_gpu", "memoria_vram"],
  "Almacenamiento": ["capacidad_disco", "tipo_almacenamiento", "interfaz_disco"],
  "Memoria RAM": ["capacidad_ram", "velocidad_ram", "generacion_ram"],
  "Fuentes de Poder": ["potencia_psu", "certificacion_psu", "formato_psu"],
  "Gabinetes": ["tamaño_gabinete", "color_gabinete"],
  "Enfriamiento Líquido": ["tamaño_radiador", "color_aio"],
  "Ventiladores y Disipadores": ["tamaño_ventilador", "iluminacion_fan", "formato_venta_fan"],
  "Tarjetas de Red": ["generacion_wifi", "velocidad_lan"],
  "Monitores": ["tamaño_monitor", "resolucion_monitor", "tasa_refresco"],
  "Teclados": ["formato_teclado", "tipo_switch", "idioma_teclado"],
  "Ratones": ["conectividad_mouse", "peso_mouse"],
  "Auriculares": ["conectividad_headset", "plataforma_headset"],
  "Bocinas": ["conectividad_bocina", "tipo_audio_bocina"],
  "Cámaras Web": ["resolucion_webcam", "fps_webcam"],
  "Micrófonos USB": ["patron_polar_mic", "calidad_audio_mic"],
  "Discos Duros Externos": ["capacidad_externo", "tipo_disco_externo"],
  "Hubs y Estaciones": ["tipo_hub", "puertos_salida_hub"],
  "Mousepads": ["tamaño_mousepad", "material_mousepad"]
};

const NOMBRES_LEGIBLES = {
  "fabricante": "Fabricante",
  "socket_placa": "Socket / Procesador", "chipset_gama": "Chipset (Gama)", "formato_placa": "Formato (Tamaño)", "tipo_ram_placa": "Tipo de Memoria RAM", "conectividad_placa": "Conectividad Integrada", "generacion_pcie_placa": "Generación PCIe",
  "gama_cpu": "Gama de Rendimiento", "linea_cpu": "Línea de Producto", "socket_cpu": "Socket", "tecnologia_cpu": "Tecnología Gaming", "presupuesto_cpu": "Presupuesto",
  "chipset_gpu": "Marca del Chipset", "gama_resolucion": "Resolución Ideal", "memoria_vram": "Memoria (VRAM)", "serie_gpu": "Serie o Generación", "ensamblador_gpu": "Ensamblador", "enfriamiento_gpu": "Tamaño y Enfriamiento", "conector_energia_gpu": "Conectores de Energía",
  "tipo_almacenamiento": "Tipo de Tecnología", "capacidad_disco": "Capacidad", "interfaz_disco": "Interfaz / Conexión", "velocidad_lectura_disco": "Velocidad de Lectura", "formato_disco": "Formato y Diseño", "caracteristicas_disco": "Características",
  "generacion_ram": "Generación (Tipo)", "capacidad_ram": "Capacidad Total", "configuracion_ram": "Configuración de Módulos", "velocidad_ram": "Velocidad (Frecuencia)", "estetica_ram": "Estética y Diseño", "perfiles_ram": "Perfiles (Overclock)",
  "potencia_psu": "Potencia (Vataje)", "certificacion_psu": "Certificación (80 Plus)", "modularidad_psu": "Modularidad", "estandar_psu": "Estándar", "formato_psu": "Formato", "estetica_psu": "Estética",
  "tamaño_gabinete": "Tamaño / Formato", "compatibilidad_aio_gabinete": "Compatibilidad Líquida", "diseno_gabinete": "Diseño", "ventiladores_incluidos": "Ventiladores Incluidos", "iluminacion_gabinete": "Iluminación de Fábrica", "color_gabinete": "Color",
  "tamaño_radiador": "Tamaño del Radiador", "socket_aio": "Compatibilidad de Socket", "pantalla_aio": "Pantalla", "estetica_aio": "Estética", "color_aio": "Color", "conectividad_aio": "Gestión",
  "tamaño_ventilador": "Tamaño", "iluminacion_fan": "Iluminación", "formato_venta_fan": "Formato de Venta", "conexion_fan": "Conexión", "color_fan": "Color",
  "interfaz_red": "Interfaz", "conectividad_red": "Conectividad", "generacion_wifi": "Generación de Wi-Fi", "velocidad_lan": "Velocidad LAN", "antenas_red": "Antenas", "perfil_red": "Perfil",
  "tamaño_monitor": "Tamaño de Pantalla", "resolucion_monitor": "Resolución", "tasa_refresco": "Tasa de Refresco", "panel_monitor": "Panel", "forma_monitor": "Forma", "sincronizacion_monitor": "Sincronización",
  "tipo_teclado": "Tipo", "formato_teclado": "Formato", "tipo_switch": "Switch", "conectividad_teclado": "Conectividad", "iluminacion_teclado": "Iluminación", "idioma_teclado": "Idioma",
  "conectividad_mouse": "Conectividad", "peso_mouse": "Peso", "botones_mouse": "Botones", "sensor_mouse": "Sensor", "diseno_mouse": "Diseño", "iluminacion_mouse": "Iluminación",
  "conectividad_headset": "Conectividad", "formato_headset": "Formato", "sonido_headset": "Tipo de Sonido", "microfono_headset": "Micrófono", "plataforma_headset": "Compatibilidad", "iluminacion_headset": "Iluminación",
  "tipo_audio_bocina": "Tipo de Audio", "conectividad_bocina": "Conectividad", "subwoofer_bocina": "Subwoofer", "alimentacion_bocina": "Alimentación", "iluminacion_bocina": "Iluminación",
  "resolucion_webcam": "Resolución", "fps_webcam": "FPS", "enfoque_webcam": "Enfoque", "iluminacion_webcam": "Iluminación", "privacidad_webcam": "Privacidad", "conectividad_webcam": "Conectividad",
  "patron_polar_mic": "Patrón Polar", "calidad_audio_mic": "Calidad de Audio", "controles_mic": "Controles", "accesorios_mic": "Accesorios", "iluminacion_mic": "Iluminación",
  "tipo_disco_externo": "Tipo", "capacidad_externo": "Capacidad", "formato_externo": "Formato", "conectividad_externo": "Conectividad", "consola_externo": "Compatibilidad Consolas", "proteccion_externo": "Protección",
  "tipo_hub": "Tipo de Producto", "puertos_entrada_hub": "Entradas", "puertos_salida_hub": "Salidas", "energia_hub": "Entrega de Energía", "alimentacion_hub": "Alimentación", "mandos_hub": "Mandos",
  "tamaño_mousepad": "Tamaño", "superficie_mousepad": "Superficie", "material_mousepad": "Material", "iluminacion_mousepad": "Iluminación", "grosor_mousepad": "Grosor", "caracteristicas_mousepad": "Características"
};

const JERARQUIA_CATEGORIAS = {
  "Componente": [
    "Placas Madre", "Procesadores", "Tarjetas Gráficas", "Almacenamiento", "Memoria RAM",
    "Fuentes de Poder", "Gabinetes", "Enfriamiento Líquido", "Ventiladores y Disipadores", "Tarjetas de Red"
  ],
  "Periféricos": [
    "Monitores", "Teclados", "Ratones", "Auriculares", "Bocinas",
    "Cámaras Web", "Micrófonos USB", "Discos Duros Externos", "Hubs y Estaciones", "Mousepads"
  ]
};

if (typeof window !== 'undefined') {
  window.REGLAS_DE_DEPENDENCIA = REGLAS_DE_DEPENDENCIA;
  window.FILTROS_GLOBALES = FILTROS_GLOBALES;
  window.ATRIBUTOS_TARJETA = ATRIBUTOS_TARJETA;
  window.NOMBRES_LEGIBLES = NOMBRES_LEGIBLES;
  window.JERARQUIA_CATEGORIAS = JERARQUIA_CATEGORIAS;
}