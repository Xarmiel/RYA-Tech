import { Component, OnChanges, input, output } from '@angular/core';
import { ProductTechnicalFicha, TechnicalSchema, TechnicalSpecField } from './product-technical.models';
import { technicalIssues } from './product-technical-validation';

const SPEC_PLACEHOLDERS: Record<string, string> = {
  // Audio, imagen y accesorios: ejemplos de formato, no datos de un producto concreto.
  'headset:design': 'Ej. Abierto, cerrado o intraaural',
  'headset:fit': 'Ej. Circumaural o supraaural',
  'headset:driver_type': 'Ej. Dinámico o planar, según fabricante',
  'driver_mm': 'Ej. 40',
  'frequency_min_hz': 'Ej. 20',
  'frequency_max_hz': 'Ej. 20000',
  'impedance_ohm': 'Ej. 120',
  'headset:sensitivity': 'Incluye dB SPL y referencia: 1 V RMS o 1 mW',
  'headset:connections': 'Ej. Jack 3.5 mm, adaptador 6.35 mm o Bluetooth',
  'microphone_type': 'Ej. Dinámico o condensador, según fabricante',
  'audio_formats': 'Formatos declarados; deja vacío si no se publican',
  'battery_hours': 'Ej. 30; indica condiciones relevantes en variante',
  'cable_length_m': 'Ej. 1.8',
  'weight_g': 'Ej. 240',
  'platforms': 'Ej. PC, PlayStation o Xbox; indica versiones',
  'polar_pattern': 'Ej. Cardioide, omnidireccional o seleccionable',
  'microphone:connections': 'Ej. USB-C o XLR de 3 pines',
  'microphone:sensitivity': 'Ej. -36 dBV/Pa; conserva la unidad y referencia',
  'max_spl_db': 'Ej. 120; anota condiciones de medición',
  'sample_rate_khz': 'Ej. 48 o 96',
  'bit_depth': 'Ej. 24',
  'power_requirements': 'Alimentación, voltaje y adaptadores requeridos',
  'mount': 'Ej. Rosca 1/4, abrazadera o soporte incluido',
  'max_resolution': 'Ej. 3840 × 2160',
  'max_fps': 'Ej. 60; completa también los modos correspondientes',
  'video_modes': 'Ej. 4K a 30 fps; 1080p a 60 fps',
  'webcam:sensor': 'Modelo o tamaño publicado del sensor',
  'focus': 'Ej. Enfoque automático o fijo',
  'field_of_view_deg': 'Ej. 90',
  'webcam:connections': 'Tipo de puerto y versión USB requeridos',
  'zoom': 'Ej. Digital 4×; no confundir con zoom óptico',
  'mousepad:material': 'Ej. Tela, vidrio o polímero',
  'surface': 'Ej. Blanda, rígida o híbrida',
  'base': 'Ej. Goma antideslizante',
  'mousepad:width_mm': 'Ej. 450',
  'mousepad:length_mm': 'Ej. 400',
  'mousepad:thickness_mm': 'Ej. 3',
  'size_inches': 'Ej. 27',
  'resolution': 'Ej. 2560 × 1440',
  'panel': 'Ej. IPS, VA, TN u OLED',
  'refresh_hz': 'Ej. 144',
  'refresh_modes': 'Resolución y Hz admitidos por cada conexión',
  'response_ms': 'Valor publicado, sin mezclar métricas diferentes',
  'response_metric': 'Ej. GtG o MPRT y condiciones de medición',
  'brightness_nits': 'Ej. 300; indica si es típico o pico',
  'contrast': 'Ej. 1000:1 estático; indica la métrica',
  'monitor:hdr': 'Estándar o certificación publicado, si existe',
  'color_gamut': 'Ej. 99 % sRGB; indica el espacio de color',
  'adaptive_sync': 'Tecnología y condiciones declaradas por fabricante',
  'monitor:connections': 'Ej. HDMI 2.0, DisplayPort 1.4',
  'vesa': 'Ej. 100 × 100 mm',
  'rotation_deg': 'Ej. 900',
  'feedback_type': 'Ej. Engranajes, correa o direct drive',
  'torque_nm': 'Par declarado; distingue pico y sostenido en variante',
  'pedals': 'Pedales incluidos y tipo de sensor o celda de carga',
  'shifter': 'Indica si se incluye o se vende por separado',
  'compatible_accessories': 'Modelos exactos indicados por el fabricante',
  'simulation:product_type': 'Ej. Joystick, HOTAS, yoke o panel',
  'racing:product_type': 'Ej. Volante, base, pedales o palanca',
  'axes': 'Ej. 6',
  'buttons': 'Ej. 8',
  'controls': 'Palancas, selectores, interruptores y otros controles',
  'cooling_type': 'Ej. Aire, AIO o ventilador de gabinete',
  'sockets': 'Ej. AM4, AM5, LGA1700; según kit incluido',
  'radiator_mm': 'Ej. 240; formato nominal, no largo total',
  'radiator_dimensions': 'Largo × ancho × grosor reales y con ventiladores',
  'fan_count': 'Ej. 2',
  'fan_size_mm': 'Ej. 120',
  'rpm_min': 'Ej. 500',
  'rpm_max': 'Ej. 1800',
  'noise_db': 'Ej. 28; indica distancia y condiciones en variante',
  'cooler:connections': 'Ej. PWM 4 pines, ARGB 5 V de 3 pines',
  'cooler:tdp_w': 'Capacidad declarada; no equivale al consumo eléctrico',
  'lighting:product_type': 'Ej. Panel, aro o tira LED',
  'temperature_min_k': 'Ej. 3200',
  'temperature_max_k': 'Ej. 5600',
  'brightness_lm': 'Ej. 1000; no sustituye una medición en lux',
  'illuminance_lux': 'Ej. 800; completa distancia y condiciones',
  'measurement_conditions': 'Ej. Distancia de 1 m y temperatura de color usada',
  'cri': 'Valor o intervalo publicado por fabricante',
  'dimensions': 'Medidas con sus unidades y orden',
  'lighting:connections': 'Ej. USB-C, Bluetooth de control o adaptador DC',
  'switch_type': 'Ej. Switch mecánico, óptico o de membrana y modelo',
  'switch_feel': 'Ej. Lineal, táctil o clicky',
  'keyboard:form_factor': 'Ej. Completo, TKL, 75 % o 60 %',
  'polling_hz': 'Ej. 1000; puede variar entre cable e inalámbrico',
  'grip': 'Forma o agarres indicados por fabricante',
  // Procesador (CPU)
  'socket': 'Ej. LGA1156, AM4 o LGA1700',
  'generacion': 'Ej. Ryzen 7000 o Intel 14',
  'cores': 'Ej. 8',
  'threads': 'Ej. 16',
  'base_mhz': 'Ej. 3600',
  'boost_mhz': 'Ej. 5000',
  'l3_mb': 'Ej. 32',
  'tdp_w': 'Ej. 65',
  'base_w': 'Ej. 65',
  'max_w': 'Ej. 120',
  'power_notes': 'Ej. TDP base según fabricante o PBP',
  'memory_type': 'Ej. DDR4 o DDR5',

  // Placa Madre (Board)
  'chipset': 'Ej. AMDX670, B650 o Z790',
  'form_factor': 'Ej. ATX, Micro-ATX o Mini-ITX',
  'memory_slots': 'Ej. 4',
  'memory_max_gb': 'Ej. 128',
  'memory_oc_mts': 'Ej. 6000',
  'pcie': 'Ej. 1x PCIe 4.0 x16, 2x PCIe 3.0 x1',
  'storage_interfaces': 'Ej. 2x M.2 PCIe 4.0, 4x SATA III',
  'm2_pcie_version': 'Ej. PCIe 4.0 x4',
  'm2_slots': 'Ej. 2',
  'sata_ports': 'Ej. 4',
  'power_connectors': 'Ej. 1x 24-pin ATX, 1x 8-pin EPS',
  'revision': 'Ej. Rev 1.0',
  'estimated_w': 'Ej. 50',
  'ethernet_mbps': 'Ej. 1000 o 2500',

  // Gabinete (Case)
  'case:form_factor': 'Ej. ATX, Micro-ATX o Mini-ITX',
  'case:height_mm': 'Ej. 450',
  'case:length_mm': 'Ej. 420',
  'case:width_mm': 'Ej. 210',
  'psu_max_length_mm': 'Ej. 180',
  'gpu_max_length_mm': 'Ej. 340',
  'cooler_max_height_mm': 'Ej. 165',
  'max_fans': 'Ej. 6',

  // Memoria RAM
  'ram:form_factor': 'Ej. DIMM o SO-DIMM',
  'ram:capacity_gb': 'Ej. 16 o 32',
  'modules': 'Ej. 2',
  'speed_mts': 'Ej. 3200 o 6000',
  'spd_mts': 'Ej. 2400 o 4800',
  'cas': 'Ej. 16 o 30',
  'voltage_v': 'Ej. 1.35',
  'profiles': 'Ej. XMP 2.0 / EXPO',
  'ram:height_mm': 'Ej. 34 o 42',
  'ram:power_w': 'Ej. 5',

  // Tarjeta Gráfica (GPU)
  'interface': 'Ej. PCIe 4.0 x16',
  'vram_gb': 'Ej. 8, 12 o 16',
  'gpu:memory_type': 'Ej. GDDR6 o GDDR6X',
  'gpu:length_mm': 'Ej. 285',
  'gpu:height_mm': 'Ej. 125',
  'thickness_mm': 'Ej. 40',
  'slots': 'Ej. 2 o 2.5',
  'board_w': 'Ej. 220',
  'recommended_psu_w': 'Ej. 650',
  'gpu:power_connectors': 'Ej. 2x 8-pin PCIe o 1x 12VHPWR',
  'video_outputs': 'Ej. 3x DP 1.4a, 1x HDMI 2.1',

  // Fuente de Poder (PSU)
  'continuous_w': 'Ej. 650, 750 o 850',
  'psu:form_factor': 'Ej. ATX o SFX',
  'atx_version': 'Ej. ATX 2.4 o ATX 3.0',
  'efficiency': 'Ej. 80 Plus Gold o Bronze',
  'modularity': 'Ej. Full modular, Semi-modular o No modular',
  'connectors': 'Ej. 1x 24p, 2x 8p EPS, 4x 8p PCIe, 6x SATA',
  'psu:length_mm': 'Ej. 140 o 160',

  // Almacenamiento (Storage)
  'drive_type': 'Ej. SSD NVMe o HDD',
  'storage:capacity_gb': 'Ej. 500, 1000 o 2000',
  'storage:interface': 'Ej. PCIe 4.0 x4 o SATA III',
  'storage:form_factor': 'Ej. M.2 2280, 2.5 pulgadas o 3.5 pulgadas',
  'm2_key': 'Ej. M-Key o B+M Key',
  'read_mbs': 'Ej. 3500 o 7000',
  'write_mbs': 'Ej. 3000 o 6500',
  'storage:power_w': 'Ej. 4.5',

  // Periféricos (Teclado y Mouse)
  'color': 'Ej. Negro, Blanco o Gris',
  'layout': 'Ej. Español ISO o Inglés ANSI',
  'connections': 'Ej. USB-C, Bluetooth 5.0 o 2.4GHz',
  'devices': 'Ej. 3',
  'systems': 'Ej. Windows, macOS, Linux',
  'variant_notes': 'Ej. Switches mecánicos Red lineales',
  'edition': 'Ej. Estándar, Pro o Edición Especial',
  'sensor': 'Ej. Óptico Hero 25K o PixArt 3395',
  'max_dpi': 'Ej. 16000 o 26000',
  'wheel': 'Ej. Con scroll infinito o botón de tensión'
};

@Component({selector:'app-product-technical-form',templateUrl:'./product-technical-form.html',styleUrl:'./product-technical-form.scss'})
export class ProductTechnicalForm implements OnChanges {
  readonly schema=input<TechnicalSchema|null>(null);
  readonly value=input<ProductTechnicalFicha|null>(null);
  readonly disabled=input(false);
  readonly valueChange=output<ProductTechnicalFicha>();
  values:ProductTechnicalFicha['valores']={};source='';
  ngOnChanges(){const value=this.value();this.values=value?.tipo===this.schema()?.tipo?{...value?.valores}:{};this.source=value?.tipo===this.schema()?.tipo?value?.fuenteUrl??'':'';}
  setField(field:TechnicalSpecField,event:Event){
    const input=event.target as HTMLInputElement;
    const raw=input.value;
    const value=field.type==='boolean'?(raw===''?null:raw==='true'):field.type==='number'?(input.validity?.badInput?NaN:raw.trim()===''?null:Number(raw)):raw.trim()||null;
    this.values={...this.values,[field.key]:value};this.emit();
  }
  setSource(event:Event){this.source=(event.target as HTMLInputElement).value;this.emit();}
  private emit(){const schema=this.schema();if(schema)this.valueChange.emit({tipo:schema.tipo,fuenteUrl:this.source.trim()||null,valores:{...this.values}});}
  issues():string[]{
    const schema=this.schema();return schema?technicalIssues(schema,{tipo:schema.tipo,fuenteUrl:this.source,valores:this.values}):[];
  }
  canSave(){return this.issues().length===0;}

  placeholder(field:TechnicalSpecField):string{
    if (field.custom && field.example) return `Ej. ${field.example}`;
    const tipo = this.schema()?.tipo ?? '';
    const qualified = `${tipo}:${field.key}`;
    if (SPEC_PLACEHOLDERS[qualified]) return SPEC_PLACEHOLDERS[qualified];
    if (SPEC_PLACEHOLDERS[field.key]) return SPEC_PLACEHOLDERS[field.key];
    return field.type === 'number' ? (field.unit ? `Ej. 100 (${field.unit})` : 'Ej. 10') : `Ej. ${field.label}`;
  }
}
