import { describe,expect,it } from 'vitest';
import { ProductAttribute } from './product-detail.models';
import { productSpecifications,specificationSource } from './product-specifications';

const attribute=(code:string,value:string,name=code,unit:string|null=null):ProductAttribute=>({code,value,name,unit,dataType:'TEXTO'});

describe('Storefront specifications without destructive changes',()=>{
  it('shows the legacy and commercial CPU fields only once, regardless of response order',()=>{
    const old=[attribute('socket_cpu','AM4','Socket'),attribute('nucleos_cpu','6','Núcleos'),attribute('cache_l3_kb','8192','Caché L3 Total','KB'),attribute('graficos_integrados_cpu','No','Gráficos integrados')];
    const manual=[attribute('CATALOG_TECH_CPU_SOCKET','AM4','Ficha · Socket'),attribute('CATALOG_TECH_CPU_CORES','6','Ficha · Núcleos'),attribute('CATALOG_TECH_CPU_L3_MB','8','Ficha · Caché L3','MB'),attribute('CATALOG_TECH_CPU_IGPU','No','Ficha · Gráficos integrados')];
    for(const rows of [[...old,...manual],[...manual,...old]]){
      const specs=productSpecifications(rows);expect(specs).toHaveLength(4);expect(specs.every(s=>s.pendingReview)).toBe(true);
      expect(specs.find(s=>s.name==='Caché L3')).toMatchObject({value:'8',unit:'MB'});expect(specs.find(s=>s.name==='Gráficos integrados')?.value).toBe('No');
      expect(specs.some(s=>s.name.startsWith('Ficha'))).toBe(false);
    }
  });
  it('separates source links from specs and hides internal type, status and migration notes',()=>{
    const rows=[attribute('CATALOG_TECH_TYPE','cpu'),attribute('CATALOG_TECH_STATUS','Pendiente de revisión; no certifica compatibilidad'),attribute('CATALOG_TECH_SOURCE','https://example.com/spec'),attribute('CATALOG_TECH_CPU_POWER_NOTES','Dato heredado potencia_base_w'),attribute('CATALOG_TECH_INTERNAL_UNKNOWN','interno')];
    expect(productSpecifications(rows)).toEqual([]);expect(specificationSource(rows)).toBe('https://example.com/spec');
  });
  it('keeps legacy-only specs, converts known KB cache units and never mutates raw data',()=>{
    const rows=[attribute('cache_l3_kb','32768','Caché L3 Total','KB'),attribute('nucleos_cpu','6','Núcleos')];
    const original=JSON.stringify(rows),specs=productSpecifications(rows);
    expect(specs[0]).toMatchObject({value:'32',unit:'MB',pendingReview:false});expect(JSON.stringify(rows)).toBe(original);
  });
  it('never converts unexpected units or deduplicates different codes just by their label',()=>{
    const specs=productSpecifications([attribute('cache_l3_kb','32','Caché','MB'),attribute('CATALOG_TECH_CPU_L3_MB','32','Ficha · Caché L3','MB'),attribute('other_cache','64','Caché','MB')]);
    expect(specs).toHaveLength(3);expect(specs[0].value).toBe('32');
  });
  it('selects the manual storefront override without pretending it is verified',()=>{
    const specs=productSpecifications([attribute('nucleos_cpu','6','Núcleos'),attribute('CATALOG_TECH_CPU_CORES','8','Ficha · Núcleos')]);
    expect(specs).toHaveLength(1);expect(specs[0]).toMatchObject({value:'8',pendingReview:true});
  });
  it('keeps TDP, base power and maximum power separate rather than equating them',()=>{
    const specs=productSpecifications([attribute('potencia_base_w','65','Potencia Base (TDP)','W'),attribute('CATALOG_TECH_CPU_BASE_W','65','Ficha · Potencia base publicada','W'),attribute('CATALOG_TECH_CPU_TDP_W','65','Ficha · TDP declarado','W'),attribute('CATALOG_TECH_CPU_MAX_W','88','Ficha · Potencia máxima publicada','W')]);
    expect(specs).toHaveLength(3);expect(specs.map(s=>s.code)).toContain('CATALOG_TECH_CPU_MAX_W');
  });
  it('deduplicates motherboard equivalents but keeps unrelated extra ports and peripherals',()=>{
    const specs=productSpecifications([attribute('socket_placa','AM4','Socket'),attribute('CATALOG_TECH_BOARD_SOCKET','AM4','Ficha · Socket'),attribute('wifi_integrado_placa','Sí','Wi-Fi'),attribute('CATALOG_TECH_BOARD_WIFI','No','Ficha · Wi-Fi'),attribute('puertos_sata_totales','6','Puertos SATA'),attribute('CATALOG_TECH_MOUSE_MAX_DPI','8000','Ficha · Resolución máxima','DPI')]);
    expect(specs).toHaveLength(4);expect(specs.find(s=>s.code==='CATALOG_TECH_BOARD_WIFI')?.value).toBe('No');
  });
  it('keeps explicit zero and false text and excludes blank values',()=>{
    const specs=productSpecifications([attribute('unknown_watts','0','Potencia','W'),attribute('custom_boolean','No','Gráficos'),attribute('empty','  ')]);
    expect(specs.map(s=>s.value)).toEqual(['0','No']);
  });
  it.each(['javascript:alert(1)','http://example.com','https://user:secret@example.com','not a url',''])('does not create unsafe source links: %s',url=>{
    expect(specificationSource([attribute('CATALOG_TECH_SOURCE',url)])).toBeNull();
  });
});
