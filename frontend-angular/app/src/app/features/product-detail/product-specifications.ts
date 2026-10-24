import { ProductAttribute } from './product-detail.models';

export interface DisplaySpecification extends ProductAttribute { pendingReview:boolean }

// Exact equivalences only: two similarly named attributes need not describe the same property.
const legacyKeys:Record<string,string>={
  socket_cpu:'cpu:socket',nucleos_cpu:'cpu:cores',hilos_cpu:'cpu:threads',
  frecuencia_base_mhz:'cpu:base_mhz',frecuencia_maxima_mhz:'cpu:boost_mhz',
  cache_l3_kb:'cpu:l3_mb',potencia_base_w:'cpu:base_w',graficos_integrados_cpu:'cpu:igpu',
  socket_placa:'board:socket',chipset_placa:'board:chipset',formato_placa:'board:form_factor',
  tipo_ram_placa:'board:memory_type',ranuras_ram_placa:'board:memory_slots',
  capacidad_ram_max_placa_gb:'board:memory_max_gb',ranura_pcie_principal:'board:pcie',
  ranuras_m2_totales:'board:m2_slots',puertos_sata_totales:'board:sata_ports',
  wifi_integrado_placa:'board:wifi',bluetooth_integrado_placa:'board:bluetooth',
  velocidad_ethernet_mbps:'board:ethernet_mbps',velocidad_ram_max_placa_mt_s:'board:memory_oc_mts',
};
const hiddenCodes=new Set(['CATALOG_TECH_TYPE','CATALOG_TECH_STATUS','CATALOG_TECH_SOURCE']);

/** Presentation only. The input, stored attributes and compatibility records remain untouched. */
export function productSpecifications(attributes:readonly ProductAttribute[]):DisplaySpecification[]{
  const result=new Map<string,DisplaySpecification>();
  for(const original of attributes){
    if(hiddenCodes.has(original.code)||/^CATALOG_TECH_.*_POWER_NOTES$/.test(original.code))continue;
    const commercial=/^CATALOG_TECH_(CPU|BOARD|RAM|STORAGE|GPU|PSU|KEYBOARD|MOUSE)_(.+)$/.exec(original.code);
    // Unknown internal namespace entries must not leak implementation metadata to the storefront.
    if(original.code.startsWith('CATALOG_TECH_')&&!commercial)continue;
    const attribute={...original,pendingReview:!!commercial};
    if(!attribute.value?.trim())continue;
    let key=commercial?`${commercial[1].toLowerCase()}:${commercial[2].toLowerCase()}`:legacyKeys[attribute.code]??`raw:${attribute.code}`;
    if(attribute.code==='cache_l3_kb'){
      if(attribute.unit?.toUpperCase()==='KB'&&/^\d+(?:\.\d+)?$/.test(attribute.value)){
        attribute.value=String(Number(attribute.value)/1024);attribute.unit='MB';attribute.name='Caché L3';
      }else key=`raw:${attribute.code}`; // Do not infer a conversion from an unrelated unit.
    }
    if(commercial)attribute.name=attribute.name.replace(/^Ficha\s*[·:]\s*/i,'');
    // A manually saved commercial value is the storefront override, always labeled as pending.
    // Its certified/legacy counterpart stays intact and remains available in administration.
    const previous=result.get(key);
    if(!previous||attribute.pendingReview&&!previous.pendingReview)result.set(key,attribute);
  }
  return [...result.values()];
}

export function specificationSource(attributes:readonly ProductAttribute[]):string|null{
  const value=attributes.find(attribute=>attribute.code==='CATALOG_TECH_SOURCE')?.value?.trim();
  if(!value)return null;
  try{const url=new URL(value);return url.protocol==='https:'&&!url.username&&!url.password?url.href:null;}catch{return null;}
}
