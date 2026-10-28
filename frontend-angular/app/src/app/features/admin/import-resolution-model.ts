import { ImportProduct } from './product-import-model';
import { ProductTechnicalFicha } from './product-technical.models';

export type ResolutionMode = 'conservar' | 'completar' | 'actualizar';
export interface ImportSnapshot {
  revision: string;
  actual: {id:string;sku:string;nombre:string;descripcion?:string|null;precioBase:number;costoReferencia?:number|null;
    categoriaSlug:string;marcaSlug?:string;fichaTecnica?:ProductTechnicalFicha|null;
    imagenes:{url:string;principal?:boolean}[]};
}
export const resolutionFields = [
  {key:'nombre',label:'Nombre'}, {key:'descripcion',label:'Descripción'}, {key:'precioBase',label:'Precio (S/)'},
  {key:'costoReferencia',label:'Costo de referencia (S/)'}, {key:'fichaTecnica',label:'Ficha técnica comercial'}, {key:'imagenes',label:'Imágenes'},
] as const;
export type ResolutionField = typeof resolutionFields[number]['key'];
export function resolutionData(product:ImportProduct) {
  return {nombre:product.nombre,descripcion:product.descripcion,precioBase:product.precio,costoReferencia:product.costoReferencia,
    fichaTecnica:product.fichaTecnica,imagenes:product.imagenes.map((url,posicion)=>({url,posicion,principal:posicion===0,textoAlternativo:product.nombre}))};
}
const missing = (value:unknown) => value==null || typeof value==='string' && !value.trim();
function cleanFicha(ficha:ProductTechnicalFicha|null|undefined) {
  return ficha ? {tipo:ficha.tipo,fuenteUrl:ficha.fuenteUrl||null,valores:ficha.valores} : null;
}
export function plannedValue(snapshot:ImportSnapshot,product:ImportProduct,mode:ResolutionMode,key:ResolutionField):unknown {
  const current=snapshot.actual[key], incoming=resolutionData(product)[key];
  if(mode==='conservar')return current;
  if(mode==='actualizar')return incoming;
  if(key==='imagenes'){
    const urls=snapshot.actual.imagenes.map(image=>image.url);
    return [...urls,...product.imagenes.filter(url=>!urls.some(old=>old.trim().toLowerCase()===url.trim().toLowerCase()))].map(url=>({url}));
  }
  if(key==='fichaTecnica'){
    const old=snapshot.actual.fichaTecnica, next=product.fichaTecnica;
    if(!old||!next)return old||next;
    const valores={...old.valores};
    for(const [key,value] of Object.entries(next.valores))if(missing(valores[key])&&!missing(value))valores[key]=value;
    return {tipo:old.tipo,fuenteUrl:old.fuenteUrl||next.fuenteUrl,valores};
  }
  return missing(current)?incoming:current;
}
export function displayValue(key:ResolutionField,value:unknown):string {
  if(missing(value))return 'Sin datos';
  if(key==='imagenes')return (value as {url:string}[]).map(image=>image.url).join('\n')||'Sin imágenes';
  if(key==='fichaTecnica')return JSON.stringify(cleanFicha(value as ProductTechnicalFicha),null,2);
  return String(value);
}
export function resolutionChanges(snapshot:ImportSnapshot,product:ImportProduct,mode:ResolutionMode) {
  return resolutionFields.map(field=>{
    const before=displayValue(field.key,snapshot.actual[field.key]);
    const value=plannedValue(snapshot,product,mode,field.key);
    const after=displayValue(field.key,value);
    return {...field,before,after,changed:before!==after,available:resolutionData(product)[field.key]!==undefined};
  });
}
