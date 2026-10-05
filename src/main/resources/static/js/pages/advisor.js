

(function () {
  'use strict';

  const GABINETES = {
    minimal_white: {
      nombre: 'NZXT H5 Flow Compact Matte White',
      precio: 360.00,
      svg: `<svg viewBox="0 0 200 280" width="180" height="250" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="caseGradW" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#FFFFFF" />
            <stop offset="100%" stop-color="#DCE3EC" />
          </linearGradient>
          <linearGradient id="glassGradW" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#071326" stop-opacity="0.9" />
            <stop offset="100%" stop-color="#020712" stop-opacity="0.95" />
          </linearGradient>
        </defs>
        <!-- Chasis Exterior -->
        <rect x="25" y="15" width="150" height="245" rx="8" fill="url(#caseGradW)" stroke="#00D4FF" stroke-width="1.5" />
        <!-- Panel lateral de vidrio templado -->
        <rect x="35" y="28" width="130" height="195" rx="4" fill="url(#glassGradW)" stroke="#C2C5CC" stroke-width="0.8" opacity="0.95" />
        <!-- Luz RGB interna Cyan RYA -->
        <line x1="45" y1="36" x2="45" y2="215" stroke="#00D4FF" stroke-width="3" stroke-linecap="round" filter="drop-shadow(0 0 6px #00D4FF)" />
        <circle cx="100" cy="85" r="28" fill="none" stroke="#00D4FF" stroke-width="2.5" opacity="0.8" stroke-dasharray="12 4" />
        <circle cx="100" cy="85" r="10" fill="#00D4FF" opacity="0.6" />
        <!-- GPU visualizada en vidrio -->
        <rect x="52" y="130" width="100" height="26" rx="3" fill="#0A2B5E" stroke="#00D4FF" stroke-width="1" />
        <line x1="62" y1="143" x2="142" y2="143" stroke="#00D4FF" stroke-width="1.5" stroke-dasharray="4 2" />
        <!-- Cubierta PSU inferior -->
        <rect x="35" y="228" width="130" height="24" rx="2" fill="#030914" />
        <text x="100" y="244" fill="#00D4FF" font-family="'Space Grotesk', sans-serif" font-size="9" font-weight="bold" text-anchor="middle" letter-spacing="1">RYA FLOW</text>
        <!-- Patas -->
        <rect x="38" y="260" width="18" height="8" rx="2" fill="#202530" />
        <rect x="144" y="260" width="18" height="8" rx="2" fill="#202530" />
      </svg>`
    },
    stealth_black: {
      nombre: 'Corsair 4000D Airflow High Airflow Black',
      precio: 390.00,
      svg: `<svg viewBox="0 0 200 280" width="180" height="250" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="caseGradB" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#141923" />
            <stop offset="100%" stop-color="#05080E" />
          </linearGradient>
          <linearGradient id="glassGradB" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#08101E" stop-opacity="0.88" />
            <stop offset="100%" stop-color="#020408" stop-opacity="0.96" />
          </linearGradient>
        </defs>
        <!-- Chasis Exterior -->
        <rect x="25" y="15" width="150" height="245" rx="8" fill="url(#caseGradB)" stroke="#3A4454" stroke-width="1.5" />
        <!-- Malla Frontal / Detalle -->
        <line x1="28" y1="25" x2="28" y2="245" stroke="#00D4FF" stroke-width="2" opacity="0.7" />
        <!-- Panel de vidrio templado tintado -->
        <rect x="36" y="26" width="130" height="198" rx="4" fill="url(#glassGradB)" stroke="#00D4FF" stroke-width="1" />
        <!-- Disipador / Líquida AIO con iluminación dual -->
        <circle cx="95" cy="80" r="26" fill="none" stroke="#00D4FF" stroke-width="3" filter="drop-shadow(0 0 8px #00D4FF)" />
        <circle cx="95" cy="80" r="14" fill="#0A2B5E" stroke="#C2C5CC" stroke-width="1" />
        <!-- Tarjeta de video con soporte -->
        <rect x="50" y="132" width="106" height="28" rx="4" fill="#0A182E" stroke="#00D4FF" stroke-width="1.5" />
        <circle cx="75" cy="146" r="9" fill="none" stroke="#00D4FF" stroke-width="1.5" />
        <circle cx="105" cy="146" r="9" fill="none" stroke="#00D4FF" stroke-width="1.5" />
        <circle cx="135" cy="146" r="9" fill="none" stroke="#00D4FF" stroke-width="1.5" />
        <!-- Módulos RAM iluminados -->
        <rect x="128" y="55" width="4" height="25" rx="1" fill="#00D4FF" filter="drop-shadow(0 0 3px #00D4FF)" />
        <rect x="135" y="55" width="4" height="25" rx="1" fill="#00D4FF" filter="drop-shadow(0 0 3px #00D4FF)" />
        <!-- PSU Shroud -->
        <rect x="36" y="228" width="130" height="24" rx="2" fill="#0B0F17" />
        <text x="100" y="244" fill="#C2C5CC" font-family="'Space Grotesk', sans-serif" font-size="9" font-weight="700" text-anchor="middle" letter-spacing="1">CORSAIR AIR</text>
        <!-- Patas -->
        <rect x="36" y="260" width="16" height="8" rx="2" fill="#1B2230" />
        <rect x="148" y="260" width="16" height="8" rx="2" fill="#1B2230" />
      </svg>`
    },
    rgb_panoramic: {
      nombre: 'Lian Li O11 Dynamic EVO Dual Chamber Black',
      precio: 620.00,
      svg: `<svg viewBox="0 0 200 280" width="180" height="250" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="o11Grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#091424" />
            <stop offset="100%" stop-color="#02050A" />
          </linearGradient>
        </defs>
        <!-- Chasis panorámico ancho -->
        <rect x="20" y="18" width="160" height="240" rx="6" fill="url(#o11Grad)" stroke="#00D4FF" stroke-width="1.8" filter="drop-shadow(0 0 10px rgba(0,212,255,0.25))" />
        <!-- Vidrio frontal y lateral continuo -->
        <rect x="26" y="24" width="148" height="216" rx="4" fill="#030814" opacity="0.92" stroke="rgba(255,255,255,0.1)" stroke-width="0.8" />
        <!-- Ventiladores Triples Laterales con aro Cyan -->
        <circle cx="150" cy="55" r="16" fill="none" stroke="#00D4FF" stroke-width="2" filter="drop-shadow(0 0 4px #00D4FF)" />
        <circle cx="150" cy="98" r="16" fill="none" stroke="#00D4FF" stroke-width="2" filter="drop-shadow(0 0 4px #00D4FF)" />
        <circle cx="150" cy="141" r="16" fill="none" stroke="#00D4FF" stroke-width="2" filter="drop-shadow(0 0 4px #00D4FF)" />
        <!-- Bloque CPU Líquida con Display -->
        <rect x="68" y="70" width="36" height="36" rx="8" fill="#0A2B5E" stroke="#00D4FF" stroke-width="2" />
        <text x="86" y="92" fill="#00D4FF" font-family="'Space Grotesk', sans-serif" font-size="10" font-weight="bold" text-anchor="middle">38°C</text>
        <!-- GPU Vertical Masiva -->
        <rect x="42" y="148" width="88" height="34" rx="4" fill="#0F1E36" stroke="#00D4FF" stroke-width="2" />
        <line x1="50" y1="165" x2="122" y2="165" stroke="#FFFFFF" stroke-width="1.5" opacity="0.8" />
        <!-- Base de soporte -->
        <rect x="32" y="258" width="136" height="8" rx="2" fill="#162030" stroke="#00D4FF" stroke-width="1" />
      </svg>`
    },
    compact_office: {
      nombre: 'Cooler Master MasterBox Q300L Micro-ATX',
      precio: 220.00,
      svg: `<svg viewBox="0 0 200 280" width="180" height="250" xmlns="http://www.w3.org/2000/svg">
        <rect x="30" y="25" width="140" height="230" rx="8" fill="#0A111E" stroke="#C2C5CC" stroke-width="1.2" />
        <!-- Patrón magnético frontal con filtro -->
        <rect x="38" y="35" width="124" height="205" rx="5" fill="#04070D" stroke="#00D4FF" stroke-width="0.8" stroke-dasharray="6 4" />
        <!-- Ventana acrílica -->
        <rect x="46" y="48" width="108" height="150" rx="4" fill="#071326" opacity="0.9" />
        <!-- Cooler CPU de perfil torre -->
        <rect x="80" y="75" width="38" height="42" rx="3" fill="#1C2D47" stroke="#00D4FF" stroke-width="1" />
        <line x1="84" y1="85" x2="114" y2="85" stroke="#00D4FF" stroke-width="1.5" />
        <line x1="84" y1="95" x2="114" y2="95" stroke="#00D4FF" stroke-width="1.5" />
        <line x1="84" y1="105" x2="114" y2="105" stroke="#00D4FF" stroke-width="1.5" />
        <!-- GPU básica / expansión -->
        <rect x="56" y="140" width="86" height="20" rx="2" fill="#0D1F38" stroke="#3A5278" stroke-width="1" />
        <!-- Panel I/O lateral -->
        <circle cx="160" cy="50" r="3" fill="#00D4FF" />
        <rect x="157" y="60" width="5" height="10" rx="1" fill="#C2C5CC" />
        <!-- Patas -->
        <rect x="42" y="255" width="20" height="8" rx="2" fill="#1F2836" />
        <rect x="138" y="255" width="20" height="8" rx="2" fill="#1F2836" />
      </svg>`
    }
  };

  const PERFILES_ASESORIA = {
    sistemas: {
      profesion: 'Ingeniero de Sistemas',
      presupuestosMinimos: 2800,
      tiers: [
        {
          maxBudget: 3800,
          gabinete: GABINETES.stealth_black,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 5 7600X (6C/12T hasta 5.3GHz)', precio: 950.00, icono: 'cpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance 32GB (2x16GB) DDR5 6000MHz', precio: 490.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston KC3000 1TB NVMe PCIe 4.0 (7000 MB/s)', precio: 380.00, icono: 'ssd' },
            { tipo: 'Tarjeta Gráfica', nombre: 'NVIDIA GeForce RTX 4060 8GB GDDR6', precio: 1390.00, icono: 'gpu' }
          ],
          explicacion: 'Ideal para compilar código velozmente, ejecutar múltiples contenedores Docker en simultáneo y correr entornos virtuales gracias a los 32GB de memoria DDR5 de alta frecuencia y el bus ultrarrápido NVMe PCIe 4.0.'
        },
        {
          maxBudget: 6000,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 9 7900X (12C/24T hasta 5.6GHz)', precio: 1750.00, icono: 'cpu' },
            { tipo: 'Memoria RAM', nombre: 'Kingston Fury Beast 64GB (2x32GB) DDR5 6000MHz', precio: 920.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Samsung 990 PRO 2TB NVMe PCIe 4.0 (7450 MB/s)', precio: 780.00, icono: 'ssd' },
            { tipo: 'Tarjeta Gráfica', nombre: 'MSI GeForce RTX 4070 SUPER 12GB GDDR6X', precio: 2790.00, icono: 'gpu' }
          ],
          explicacion: 'Excelente para entrenamiento local de redes neuronales, orquestación pesada con Kubernetes, procesamiento intensivo de datos e ingeniería de software distribuida con 12 núcleos y 64GB de RAM dedicados.'
        }
      ]
    },
    civil: {
      profesion: 'Ingeniero Civil',
      presupuestosMinimos: 3200,
      tiers: [
        {
          maxBudget: 4500,
          gabinete: GABINETES.stealth_black,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i7-14700F (20 Núcleos hasta 5.4GHz)', precio: 1580.00, icono: 'cpu' },
            { tipo: 'Memoria RAM', nombre: 'Kingston Fury Beast 32GB DDR5 5600MHz', precio: 460.00, icono: 'ram' },
            { tipo: 'Tarjeta Gráfica', nombre: 'ASUS Dual GeForce RTX 4060 Ti 16GB GDDR6', precio: 1980.00, icono: 'gpu' },
            { tipo: 'Almacenamiento', nombre: 'Crucial T500 1TB NVMe Gen4 (7300 MB/s)', precio: 410.00, icono: 'ssd' }
          ],
          explicacion: 'Configuración optimizada para cálculos estructurales masivos en SAP2000, ETABS, modelado 3D BIM en Revit y AutoCAD gracias a su procesador de 20 núcleos y los 16GB de VRAM que evitan cuellos de botella en mallas complejas.'
        },
        {
          maxBudget: 7000,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i9-14900K (24 Núcleos hasta 6.0GHz)', precio: 2450.00, icono: 'cpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance 64GB (2x32GB) DDR5 6000MHz', precio: 950.00, icono: 'ram' },
            { tipo: 'Tarjeta Gráfica', nombre: 'Gigabyte GeForce RTX 4070 Ti SUPER 16GB GDDR6X', precio: 3450.00, icono: 'gpu' },
            { tipo: 'Almacenamiento', nombre: 'WD Black SN850X 2TB NVMe PCIe 4.0', precio: 760.00, icono: 'ssd' }
          ],
          explicacion: 'Poder de cálculo extremo para simulaciones geotécnicas, análisis no lineal por elementos finitos y proyectos de infraestructura de gran envergadura en Civil 3D con fluidez en tiempo real.'
        }
      ]
    },
    arquitecto: {
      profesion: 'Arquitecto',
      presupuestosMinimos: 3400,
      tiers: [
        {
          maxBudget: 4800,
          gabinete: GABINETES.minimal_white,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i7-13700K (16C/24T hasta 5.4GHz)', precio: 1520.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'MSI GeForce RTX 4060 Ti 16GB Ventus Black', precio: 1950.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz', precio: 510.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston KC3000 1TB NVMe PCIe 4.0', precio: 380.00, icono: 'ssd' }
          ],
          explicacion: 'Ideal para modelado en Archicad/Revit y renderizado foto-realista en Lumion, V-Ray y Enscape. Su aceleración por Ray Tracing y amplia memoria VRAM aseguran previsualizaciones hiperrealistas sin congelamientos.'
        },
        {
          maxBudget: 7500,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 9 7950X (16C/32T hasta 5.7GHz)', precio: 2350.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'ASUS TUF Gaming GeForce RTX 4080 SUPER 16GB', precio: 4350.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'G.Skill Trident Z5 Neo 64GB DDR5 6000MHz', precio: 990.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Samsung 990 PRO 2TB NVMe PCIe 4.0', precio: 780.00, icono: 'ssd' }
          ],
          explicacion: 'Estación de renderizado arquitectónico masivo para Twinmotion y Unreal Engine 5 con Lumen en tiempo real, permitiendo recorridos virtuales cinematográficos en resoluciones 4K para clientes de alto nivel.'
        }
      ]
    },
    artista_grafico: {
      profesion: 'Artista Gráfico / Ilustrador',
      presupuestosMinimos: 2400,
      tiers: [
        {
          maxBudget: 3500,
          gabinete: GABINETES.minimal_white,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 5 7600 (6 Núcleos hasta 5.1GHz)', precio: 880.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'ASUS Dual GeForce RTX 4060 8GB OC', precio: 1390.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance 32GB (2x16GB) DDR5 5600MHz', precio: 460.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston NV2 1TB NVMe PCIe 4.0', precio: 290.00, icono: 'ssd' }
          ],
          explicacion: 'Perfecta para ilustración digital en Photoshop, Illustrator y Clip Studio Paint con lienzos gigantes de cientos de capas y 300+ DPI, así como modelado 3D básico en Blender sin ralentizaciones.'
        },
        {
          maxBudget: 5500,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i7-14700K (20C/28T hasta 5.6GHz)', precio: 1720.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'MSI GeForce RTX 4070 SUPER 12GB Gaming X', precio: 2890.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance RGB 64GB DDR5 6000MHz', precio: 950.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Samsung 990 PRO 2TB NVMe PCIe 4.0', precio: 780.00, icono: 'ssd' }
          ],
          explicacion: 'Configuración concebida para artistas conceptuales y concept art 3D en ZBrush, Substance Painter y renders por GPU en Octane/Cycles con texturas 8K sin caídas de framerate.'
        }
      ]
    },
    editor_video: {
      profesion: 'Editor de Video / Post-producción',
      presupuestosMinimos: 3200,
      tiers: [
        {
          maxBudget: 4600,
          gabinete: GABINETES.stealth_black,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i7-14700 (QuickSync decodificador AV1/HEVC)', precio: 1540.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'ASUS Dual GeForce RTX 4060 Ti 16GB GDDR6', precio: 1980.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Kingston Fury Beast 32GB DDR5 6000MHz', precio: 490.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Crucial T500 1TB NVMe PCIe 4.0 (Lectura 7300MB/s)', precio: 410.00, icono: 'ssd' }
          ],
          explicacion: 'Ideal para edición en Premiere Pro y DaVinci Resolve a resoluciones 4K multicámara. La combinación de Intel QuickSync y los núcleos Tensor de NVIDIA permite reproducción fluida en la línea de tiempo y exportación veloz.'
        },
        {
          maxBudget: 7200,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i9-14900K (24C/32T hasta 6.0GHz)', precio: 2450.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'MSI GeForce RTX 4070 Ti SUPER 16GB GDDR6X', precio: 3490.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance 64GB (2x32GB) DDR5 6400MHz', precio: 1050.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Samsung 990 PRO 2TB NVMe PCIe 4.0', precio: 780.00, icono: 'ssd' }
          ],
          explicacion: 'Especializada para gradación de color intensiva en DaVinci Resolve Studio, composición visual en After Effects y flujos de trabajo RAW 6K/8K sin necesidad de proxies.'
        }
      ]
    },
    videojuegos: {
      profesion: 'Desarrollador de Videojuegos',
      presupuestosMinimos: 3500,
      tiers: [
        {
          maxBudget: 4900,
          gabinete: GABINETES.stealth_black,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 7 7700X (8C/16T hasta 5.4GHz)', precio: 1390.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'ASUS Dual GeForce RTX 4070 12GB GDDR6X', precio: 2490.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'Corsair Vengeance 32GB (2x16GB) DDR5 6000MHz', precio: 490.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston KC3000 1TB NVMe PCIe 4.0', precio: 380.00, icono: 'ssd' }
          ],
          explicacion: 'Optimizada para desarrollo de videojuegos en Unity y Unreal Engine 5. Permite compilar scripts en C++, hornear iluminación y testear en tiempo real con tecnologías DLSS 3 y Ray Tracing activadas.'
        },
        {
          maxBudget: 7800,
          gabinete: GABINETES.rgb_panoramic,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 9 7950X3D (16C con 3D V-Cache)', precio: 2680.00, icono: 'cpu' },
            { tipo: 'Tarjeta Gráfica', nombre: 'Gigabyte GeForce RTX 4080 SUPER 16GB Gaming OC', precio: 4420.00, icono: 'gpu' },
            { tipo: 'Memoria RAM', nombre: 'G.Skill Trident Z5 64GB DDR5 6000MHz', precio: 980.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Samsung 990 PRO 2TB NVMe PCIe 4.0', precio: 780.00, icono: 'ssd' }
          ],
          explicacion: 'La máquina definitiva para estudios de desarrollo AAA. Compilación instantánea de shaders, horneado acústico y pruebas con simulaciones físicas avanzadas a máximos cuadros por segundo.'
        }
      ]
    },
    oficinista: {
      profesion: 'Oficinista / Trabajo Administrativo',
      presupuestosMinimos: 1600,
      tiers: [
        {
          maxBudget: 2400,
          gabinete: GABINETES.compact_office,
          componentes: [
            { tipo: 'Procesador', nombre: 'AMD Ryzen 5 5600G (6C/12T con Gráficos Radeon Vega 7)', precio: 620.00, icono: 'cpu' },
            { tipo: 'Placa Madre', nombre: 'ASUS Prime B550M-A WiFi II AMD AM4', precio: 420.00, icono: 'motherboard' },
            { tipo: 'Memoria RAM', nombre: 'Kingston Fury Beast 16GB (2x8GB) DDR4 3200MHz', precio: 220.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston NV2 500GB NVMe PCIe 4.0', precio: 190.00, icono: 'ssd' }
          ],
          explicacion: 'Equipo sumamente silencioso y eficiente para ofimática avanzada, hojas de cálculo en Excel con macros complejas, videoconferencias en Teams/Zoom y multitarea con más de 30 pestañas sin lentitud.'
        },
        {
          maxBudget: 3200,
          gabinete: GABINETES.minimal_white,
          componentes: [
            { tipo: 'Procesador', nombre: 'Intel Core i5-13400 (10C/16T con Gráficos UHD 730)', precio: 980.00, icono: 'cpu' },
            { tipo: 'Placa Madre', nombre: 'MSI B760M Bomber DDR5 Intel LGA1700', precio: 490.00, icono: 'motherboard' },
            { tipo: 'Memoria RAM', nombre: 'Crucial 32GB (2x16GB) DDR5 4800MHz', precio: 420.00, icono: 'ram' },
            { tipo: 'Almacenamiento', nombre: 'Kingston NV2 1TB NVMe PCIe 4.0', precio: 290.00, icono: 'ssd' }
          ],
          explicacion: 'Ideal para gestión corporativa, bases de datos locales, ERPs y flujos de trabajo administrativos con pantallas múltiples garantizando máxima fluidez y larga vida útil.'
        }
      ]
    }
  };

  function getComponentIcon(tipo) {
    switch (tipo) {
      case 'cpu':
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <rect x="9" y="9" width="6" height="6" />
          <path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 14h3M1 9h3M1 14h3" />
        </svg>`;
      case 'gpu':
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="5" width="20" height="14" rx="2" />
          <circle cx="8" cy="12" r="3" />
          <circle cx="16" cy="12" r="3" />
          <path d="M2 10h2M2 14h2M20 10h2M20 14h2" />
        </svg>`;
      case 'ram':
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="6" width="20" height="12" rx="2" />
          <path d="M6 10v4M10 10v4M14 10v4M18 10v4M6 18v2M10 18v2M14 18v2M18 18v2" />
        </svg>`;
      case 'ssd':
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="16" rx="2" />
          <circle cx="7" cy="8" r="1.5" fill="currentColor" />
          <path d="M7 16h10M7 12h10" />
        </svg>`;
      case 'motherboard':
      default:
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <path d="M8 8h3v3H8zM14 14h3v3h-3zM8 14h3v3H8z" />
          <path d="M14 8h3" />
        </svg>`;
    }
  }

  function formatPEN(monto) {
    return `S/ ${Number(monto || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  function calcularAsesoria(profesionKey, presupuestoIngresado) {
    const perfil = PERFILES_ASESORIA[profesionKey];
    if (!perfil) return null;

    const budget = Number(presupuestoIngresado);

    let tierElegido = perfil.tiers[0];
    for (let i = perfil.tiers.length - 1; i >= 0; i--) {
      if (budget >= perfil.tiers[i].maxBudget * 0.75) {
        tierElegido = perfil.tiers[i];
        break;
      }
    }

    const gabinete = tierElegido.gabinete;
    const componentes = tierElegido.componentes;

    const sumaComponentes = componentes.reduce((acc, curr) => acc + curr.precio, 0);
    const sumaTotal = sumaComponentes + gabinete.precio;

    const seExcede = sumaTotal > budget;

    return {
      profesionNombre: perfil.profesion,
      presupuestoIngresado: budget,
      gabinete: gabinete,
      componentes: componentes,
      sumaComponentes: sumaComponentes,
      sumaTotal: sumaTotal,
      seExcede: seExcede,
      explicacion: seExcede ? 'El presupuesto se excede' : tierElegido.explicacion
    };
  }

  function renderizarResultadoAsesoria(resultado) {
    const container = document.getElementById('advisorResultsArea');
    if (!container || !resultado) return;

    const { gabinete, componentes, sumaTotal, presupuestoIngresado, seExcede, explicacion } = resultado;
    const balance = presupuestoIngresado - sumaTotal;

    container.innerHTML = `
      <!-- Layout Principal: 4 filas a la izquierda para el gabinete, 4 filas a la derecha para 1 componente por fila -->
      <div class="advisor-showcase-grid">
        
        <!-- Gabinete escogido (Altura equivalente a las 4 filas de componentes) -->
        <div class="advisor-case-card">
          <div class="advisor-case-badge">Gabinete Seleccionado</div>
          <div class="advisor-case-image-container">
            ${gabinete.svg}
          </div>
          <div class="advisor-case-info">
            <h4 class="advisor-case-title">${gabinete.nombre}</h4>
            <span class="advisor-case-price">${formatPEN(gabinete.precio)}</span>
          </div>
        </div>

        <!-- 4 Componentes seleccionados: 1 por fila -->
        <div class="advisor-components-column">
          ${componentes.map(c => `
            <div class="advisor-component-row">
              <div class="advisor-component-left">
                <div class="advisor-component-icon-box" title="${c.tipo}">
                  ${getComponentIcon(c.icono)}
                </div>
                <div class="advisor-component-details">
                  <span class="advisor-component-type">${c.tipo}</span>
                  <span class="advisor-component-name">${c.nombre}</span>
                </div>
              </div>
              <span class="advisor-component-price">${formatPEN(c.precio)}</span>
            </div>
          `).join('')}
        </div>

      </div>

      <!-- Resumen financiero del ensamble -->
      <div class="advisor-budget-summary">
        <span class="budget-entered">Presupuesto ingresado: <strong>${formatPEN(presupuestoIngresado)}</strong></span>
        <span class="budget-total">Total estimado del ensamble: <strong>${formatPEN(sumaTotal)}</strong></span>
        <span class="budget-balance ${balance < 0 ? 'is-negative' : ''}">
          ${balance >= 0 ? 'Margen a favor:' : 'Déficit:'} <strong>${formatPEN(Math.abs(balance))}</strong>
        </span>
      </div>

      <!-- Cuadro de texto explicativo debajo de los componentes y el gabinete -->
      <div class="advisor-explanation-box ${seExcede ? 'is-warning' : 'is-success'}">
        <div class="advisor-explanation-icon">
          ${seExcede ? `
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          ` : `
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          `}
        </div>
        <div class="advisor-explanation-content">
          <div class="advisor-explanation-title">
            ${seExcede ? 'Aviso de Presupuesto' : '¿Por qué esta PC es la ideal para tu profesión?'}
          </div>
          <p class="advisor-explanation-text">${explicacion}</p>
        </div>
      </div>
    `;
  }

  function inicializarModuloAsesoria() {
    const form = document.getElementById('advisorForm');
    const inputPresupuesto = document.getElementById('advisorBudgetInput');
    const selectProfesion = document.getElementById('advisorProfessionSelect');

    if (!form || !inputPresupuesto || !selectProfesion) return;

    inputPresupuesto.addEventListener('input', function () {
      this.value = this.value.replace(/[^0-9]/g, '');
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();

      const profesion = selectProfesion.value;
      const presupuesto = parseFloat(inputPresupuesto.value);

      if (!profesion) {
        alert('Por favor, selecciona una profesión de la lista.');
        return;
      }

      if (!presupuesto || isNaN(presupuesto) || presupuesto <= 0) {
        alert('Por favor, ingresa un monto válido de presupuesto en soles.');
        inputPresupuesto.focus();
        return;
      }

      const resultado = calcularAsesoria(profesion, presupuesto);
      renderizarResultadoAsesoria(resultado);

      const resultsArea = document.getElementById('advisorResultsArea');
      if (resultsArea) {
        resultsArea.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });

    const inicial = calcularAsesoria('sistemas', 4500);
    renderizarResultadoAsesoria(inicial);
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', inicializarModuloAsesoria);
    } else {
      inicializarModuloAsesoria();
    }
  }

  if (typeof window !== 'undefined') {
    window.calcularAsesoria = calcularAsesoria;
    window.renderizarResultadoAsesoria = renderizarResultadoAsesoria;
  }
})();
