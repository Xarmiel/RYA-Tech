// Campos guía del prototipo. No son reglas definitivas de compatibilidad ni un esquema SQL.
export interface TechnicalField {id:string;label:string;unit:string;required:boolean;type:string;integer?:boolean;}
export interface TechnicalType {name:string;icon:string;fields:TechnicalField[];groups:string[];defaultUse?:string;}
export const TECHNICAL_MODEL = (() => {
  const field = (id:string, label:string, unit = '', required = false, type = 'text'):TechnicalField => ({ id, label, unit, required, type });
  const number = (id:string, label:string, unit = '', required = false):TechnicalField => ({...field(id, label, unit, required, 'number'),integer:['cores','threads','memory_slots','modules','quantity'].includes(id)});
  const types:Record<string,TechnicalType> = {
    cpu: { name:'Procesador', icon:'memory', fields:[field('socket','Socket','',true),number('cores','Núcleos','',true),number('threads','Hilos'),number('base_mhz','Frecuencia base','MHz'),number('boost_mhz','Frecuencia máxima','MHz'),number('l3_kb','Caché L3','KB'),number('base_w','Potencia base','W',true),number('max_w','Potencia máxima','W'),field('family','Familia y generación'),field('igpu','Gráficos integrados','',true,'boolean')], groups:['memory','benchmarks','cpu_board'] },
    board: { name:'Placa madre', icon:'developer_board', fields:[field('socket','Socket','',true),field('chipset','Chipset','',true),field('form_factor','Formato','',true),field('memory_type','Tipo de RAM','',true),number('memory_slots','Ranuras de RAM'),number('memory_max_gb','RAM máxima','GB'),field('power_connectors','Conectores de alimentación','',true),field('pcie','Ranuras PCIe / versiones'),field('storage_interfaces','M.2 / SATA e interfaces')], groups:['bios','cpu_board'] },
    ram: { name:'Memoria RAM', icon:'memory_alt', fields:[field('memory_type','Tipo de memoria (DDR)','',true),field('form_factor','Formato DIMM / SO-DIMM','',true),number('capacity_gb','Capacidad total','GB',true),number('modules','Número de módulos','',true),number('speed_mhz','Velocidad nominal','MHz'),number('height_mm','Altura de módulos','mm'),number('voltage_v','Voltaje','V'),field('profiles','Perfiles JEDEC / XMP / EXPO')], groups:[] },
    gpu: { name:'Tarjeta gráfica', icon:'developer_board', fields:[field('interface','Interfaz PCIe','',true),number('vram_gb','Memoria de video','GB'),number('length_mm','Largo','mm',true),number('height_mm','Alto','mm'),number('thickness_mm','Grosor','mm',true),number('slots','Ranuras ocupadas'),number('board_w','Consumo de la tarjeta','W',true),field('power_connectors','Conectores de alimentación','',true),field('video_outputs','Salidas de video')], groups:[] },
    cooling: { name:'Refrigeración', icon:'ac_unit', fields:[field('cooler_type','Tipo (aire / líquida)','',true),field('sockets','Sockets admitidos','',true),number('height_mm','Altura total','mm',true),number('radiator_mm','Tamaño del radiador','mm'),number('radiator_thickness_mm','Grosor del radiador','mm'),field('connectors','Conectores / control'),number('rated_w','Capacidad térmica publicada','W')], groups:[] },
    psu: { name:'Fuente de poder', icon:'power', fields:[number('continuous_w','Potencia continua','W',true),field('form_factor','Formato ATX / SFX','',true),number('length_mm','Largo','mm',true),field('atx_version','Versión ATX'),field('efficiency','Certificación de eficiencia'),field('modularity','Modularidad')], groups:['connectors'] },
    case: { name:'Gabinete', icon:'desktop_windows', fields:[field('board_formats','Formatos de placa admitidos','',true),field('psu_formats','Formatos de fuente admitidos','',true),number('gpu_max_mm','Largo máximo de GPU','mm',true),number('cooler_max_mm','Altura máxima de disipador','mm',true),number('psu_max_mm','Largo máximo de fuente','mm'),field('radiator_positions','Radiadores y posiciones admitidas'),field('drive_bays','Bahías de almacenamiento'),field('front_connectors','Conexiones frontales')], groups:[] },
    storage: { name:'Almacenamiento', icon:'storage', fields:[field('drive_type','Tipo SSD / HDD','',true),field('interface','Interfaz SATA / PCIe y versión','',true),field('form_factor','Formato y dimensiones','',true),number('capacity_gb','Capacidad','GB'),field('m2_key','Clave M.2, cuando corresponda'),number('power_w','Consumo publicado','W')], groups:[] },
    peripheral: { name:'Periférico', icon:'keyboard', fields:[field('peripheral_type','Tipo de periférico'),field('connection','Conexión / interfaz'),field('os_support','Sistemas admitidos'),field('requirements','Requisitos de puertos / software'),number('power_w','Alimentación publicada','W')], groups:[], defaultUse:'catalog' },
    other: { name:'Otro / software', icon:'category', fields:[field('product_type','Tipo de producto'),field('platform','Plataforma / sistema'),field('requirements','Requisitos publicados')], groups:[], defaultUse:'catalog' },
  };
  const groups:Record<string,{name:string;note:string;fields:TechnicalField[]}> = {
    memory:{name:'Soporte de memoria del procesador',note:'Un registro por perfil publicado; no confundir frecuencias oficiales con overclock.',fields:[field('type','Tipo de RAM'),number('speed_mhz','Velocidad','MHz'),field('configuration','Configuración de módulos / rangos'),field('source_url','Fuente HTTPS','',false,'url')]},
    benchmarks:{name:'Benchmarks por aplicación',note:'Opcional. No inventar puntuaciones: indicar versión, entorno y fuente.',fields:[field('application','Aplicación y versión'),number('score','Resultado'),field('unit','Unidad'),field('environment','Entorno de prueba'),field('source_url','Fuente HTTPS','',false,'url')]},
    bios:{name:'Revisiones de placa y BIOS',note:'Cada versión debe asociarse a una revisión concreta de la placa.',fields:[field('revision','Revisión de hardware'),field('version','Versión BIOS'),field('date','Fecha publicada','',false,'date'),field('source_url','Fuente HTTPS','',false,'url')]},
    cpu_board:{name:'Relación CPU ↔ placa madre',note:'Relación propuesta, pendiente de revisión. Compartir socket no basta para verificar compatibilidad.',fields:[field('cpu_sku','SKU / MPN de CPU'),field('board_sku','SKU / MPN de placa'),field('revision','Revisión de placa'),field('minimum_bios','BIOS mínima'),field('source_url','Listado oficial de soporte HTTPS','',false,'url')]},
    connectors:{name:'Conectores de la fuente',note:'Registrar tipo y cantidad; no considerar equivalentes conectores distintos.',fields:[field('connector','Tipo de conector'),number('quantity','Cantidad')]},
  };
  function shortcut(event:Pick<KeyboardEvent,'ctrlKey'|'shiftKey'|'altKey'|'metaKey'|'repeat'|'code'|'key'>) {
    return event.ctrlKey && event.shiftKey && !event.altKey && !event.metaKey && !event.repeat
      && (['Minus','NumpadSubtract'].includes(event.code) || ['-','_'].includes(event.key));
  }
  function numberValue(value:unknown) { return typeof value === 'string' && !value.trim() ? null : Number(value); }
  function valueOf(definition:TechnicalField, raw:unknown) {
    if (raw === '' || raw == null) return null;
    if (definition.type === 'number') return numberValue(raw);
    if (definition.type === 'boolean') return raw === 'true' || raw === true ? true : raw === 'false' || raw === false ? false : null;
    return String(raw).trim() || null;
  }
  return { types, groups, shortcut, valueOf };
})();
