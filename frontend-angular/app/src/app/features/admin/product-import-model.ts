import { ProductTechnicalFicha } from './product-technical.models';

export interface ImportProduct {
  nombre: string; sku: string; marca: string; categoria: string; mpn: string;
  precio: number; stockInicial: number; descripcion: string; imagenes: string[];
  errors: string[]; warnings: string[];
  fichaTecnica?: ProductTechnicalFicha;
  camposFicha?: string[];
  accion?: 'crear'|'actualizar-ficha'|'agregar-imagenes';
  slug?: string;
  costoReferencia?: number;
  estado?: string;
  requiereSerie?: boolean;
  requiereEnvio?: boolean;
}

export const importKey = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
const text = (value: unknown): string => typeof value === 'string' ? value.trim() : '';
function number(value: unknown): number {
  return (typeof value === 'number' || typeof value === 'string' && value.trim() !== '') ? Number(value) : NaN;
}

/** CSV UTF-8, comma separated, including quoted fields and embedded newlines. */
export function readCsv(source: string): Record<string, string>[] {
  const rows: string[][] = []; let row: string[] = [], field = '', quoted = false, closed = false;
  source = source.replace(/^\uFEFF/, '');
  for (let i = 0; i < source.length; i++) {
    const c = source[i];
    if (quoted) {
      if (c === '"' && source[i + 1] === '"') { field += '"'; i++; }
      else if (c === '"') { quoted = false; closed = true; }
      else field += c;
    } else if (c === '"') {
      if (field || closed) throw new Error('Comillas CSV mal formadas.');
      quoted = true;
    } else if (c === ',' || c === '\n' || c === '\r') {
      row.push(field); field = ''; closed = false;
      if (c !== ',') {
        if (c === '\r' && source[i + 1] === '\n') i++;
        if (row.some(v => v.trim())) rows.push(row);
        row = [];
      }
    } else {
      if (closed) throw new Error('Texto después de cerrar comillas CSV.');
      field += c;
    }
  }
  if (quoted) throw new Error('Hay comillas sin cerrar en el CSV.');
  row.push(field); if (row.some(v => v.trim())) rows.push(row);
  const header = rows.shift()?.map(v => v.trim()) ?? [];
  if (!header.includes('sku') || !header.includes('nombre') || new Set(header).size !== header.length || header.some(v => !v))
    throw new Error('CSV: usa cabeceras únicas, incluyendo sku y nombre, separadas por comas.');
  return rows.map((values, i) => {
    if (values.length !== header.length) throw new Error(`CSV: cantidad de columnas incorrecta en registro ${i + 2}.`);
    return Object.fromEntries(header.map((key, j) => [key, values[j]]));
  });
}

export function parseProducts(source: string, format: 'json' | 'csv'): ImportProduct[] {
  if (new TextEncoder().encode(source).length > 2 * 1024 * 1024) throw new Error('El archivo supera 2 MB.');
  const document = format === 'json' ? JSON.parse(source.replace(/^\uFEFF/, '')) : readCsv(source);
  if(document?.modo && !['crear-borradores','crear-y-actualizar-fichas','agregar-imagenes'].includes(document.modo))throw new Error('Este archivo es un respaldo de consulta, no un lote para crear productos.');
  const items = Array.isArray(document) ? document : document?.productos;
  if (!Array.isArray(items) || !items.length || items.length > 100) throw new Error('Incluye entre 1 y 100 productos.');
  const products = items.map((item: any): ImportProduct => {
    if (!item || typeof item !== 'object' || Array.isArray(item)) throw new Error('Cada producto debe ser un objeto.');
    const errors: string[] = [], warnings: string[] = [];
    const rawImages = item.imagenesCandidatas ?? item.imagenes ?? Array.from({length: 8}, (_, i) => item[`imagen${i + 1}`]).filter(Boolean);
    if (!Array.isArray(rawImages)) errors.push('La galería debe ser una lista.');
    const imagenes = Array.isArray(rawImages) ? rawImages.map((image: any) => text(typeof image === 'string' ? image : image?.url)) : [];
    const product = {
      nombre: text(item.nombre), sku: text(item.skuPropuesto ?? item.sku),
      slug: text(item.slug),
      marca: text(item.marca), categoria: text(item.categoriaPropuesta ?? item.categoria), mpn: text(item.mpn),
      precio: number(item.precioDemoPEN ?? item.precio),
      costoReferencia: Number.isFinite(number(item.costoReferencia)) ? number(item.costoReferencia) : undefined,
      stockInicial: number(item.stockInicialDemo ?? item.stockInicial ?? item.stock),
      estado: typeof item.estado === 'string' && item.estado.trim() ? item.estado.trim() : undefined,
      requiereSerie: typeof item.requiereSerie === 'boolean' ? item.requiereSerie : undefined,
      requiereEnvio: typeof item.requiereEnvio === 'boolean' ? item.requiereEnvio : undefined,
      descripcion: text(item.descripcion), imagenes, errors, warnings,
      accion:item.accion==='actualizar-ficha'?'actualizar-ficha' as const:item.accion==='agregar-imagenes'?'agregar-imagenes' as const:'crear' as const,
    };
    if (!product.nombre || product.nombre.length > 220) errors.push('Nombre obligatorio, máximo 220 caracteres.');
    if (!product.sku || product.sku.length > 80) errors.push('SKU obligatorio, máximo 80 caracteres.');
    if (product.accion==='crear'&&(!Number.isFinite(product.precio) || product.precio < 0 || product.precio > 99999999.99 || Math.abs(product.precio * 100 - Math.round(product.precio * 100)) > 0.00001))
      errors.push('Precio en soles: número no negativo, máximo dos decimales; usa punto decimal.');
    if (product.accion==='crear'&&(!Number.isInteger(product.stockInicial) || product.stockInicial < 0 || product.stockInicial > 2147483647)) errors.push('Stock obligatorio: entero no negativo.');
    if (imagenes.length > 8) errors.push('Máximo ocho imágenes.');
    if (new Set(imagenes.map(url => url.toLowerCase())).size !== imagenes.length) errors.push('Imágenes repetidas.');
    if (imagenes.some(url => { try { const u = new URL(url); return u.protocol !== 'https:' || !!u.username || !!u.password; } catch { return true; } })) errors.push('Imágenes: enlaces HTTPS válidos sin credenciales.');
    if (!imagenes.length&&product.accion!=='actualizar-ficha') warnings.push('Sin imágenes.');
    if(item.fichaTecnica!==undefined){
      const ficha=item.fichaTecnica;
      if(!ficha||typeof ficha!=='object'||Array.isArray(ficha)||typeof ficha.tipo!=='string'||!ficha.valores||typeof ficha.valores!=='object'||Array.isArray(ficha.valores)
        ||ficha.fuenteUrl!=null&&typeof ficha.fuenteUrl!=='string'||Object.values(ficha.valores).some(v=>v!==null&&!['string','number','boolean'].includes(typeof v)))errors.push('Ficha técnica: usa un objeto con tipo, fuenteUrl opcional y valores simples.');
      else (product as ImportProduct).fichaTecnica={tipo:ficha.tipo,fuenteUrl:ficha.fuenteUrl??null,valores:{...ficha.valores}};
      warnings.push('La ficha comercial se guardará pendiente de revisión. No certifica compatibilidad.');
    }
    if (item.especificacionesVerificadas || item.compatibilidad || item.fuentes) warnings.push('Los campos antiguos especificacionesVerificadas, compatibilidad y fuentes NO se importan. Solo se guarda el objeto fichaTecnica del nuevo formato.');
    if(item.accion!==undefined&&!['crear','actualizar-ficha','agregar-imagenes'].includes(item.accion))errors.push('Acción desconocida. Usa crear, actualizar-ficha o agregar-imagenes.');
    if(product.accion==='actualizar-ficha'){
      if(!(product as ImportProduct).fichaTecnica)errors.push('Actualizar ficha requiere fichaTecnica.');
      warnings.push('ACTUALIZAR SOLO FICHA del SKU exacto. Precio, stock, imágenes y publicación se conservan.');
    }
    if(item.camposFicha!==undefined){
      const fields=item.camposFicha;
      if(product.accion!=='actualizar-ficha'||!Array.isArray(fields)||!fields.length||fields.length>60||fields.some((key:unknown)=>typeof key!=='string'||!key.trim())||new Set(fields).size!==fields.length
        ||!((product as ImportProduct).fichaTecnica)||Object.keys((product as ImportProduct).fichaTecnica!.valores).length!==fields.length||fields.some((key:string)=>!Object.hasOwn((product as ImportProduct).fichaTecnica!.valores,key)))
        errors.push('camposFicha requiere actualizar-ficha y una lista única con exactamente las claves incluidas en fichaTecnica.valores.');
      else{(product as ImportProduct).camposFicha=[...fields];warnings.push('Actualización parcial: solo se reemplazan los campos indicados; se conservan la fuente y los demás valores actuales de la ficha.');}
    }
    if(product.accion==='agregar-imagenes'){
      if(!imagenes.length)errors.push('Incluye al menos una imagen.');
      if(item.fichaTecnica!==undefined)errors.push('Agregar imágenes no acepta fichaTecnica; usa acciones separadas.');
      warnings.push('AGREGAR SOLO IMÁGENES al SKU exacto. Se conservan las existentes y la principal actual; los enlaces ya guardados no se duplican. Máximo ocho en total.');
    }
    return product;
  });
  for (const p of products) {
    if (products.some(other => other !== p && (importKey(other.sku) === importKey(p.sku) || importKey(other.nombre) === importKey(p.nombre) || !!p.mpn && importKey(p.mpn) === importKey(other.mpn))))
      p.errors.push('SKU, nombre o MPN repetido dentro del archivo.');
  }
  return products;
}

export interface CatalogIdentity { code: string; title: string }
export function findDuplicate(product: ImportProduct, existing: CatalogIdentity[]): CatalogIdentity | undefined {
  return existing.find(item => importKey(item.code) === importKey(product.sku)
    || importKey(item.title) === importKey(product.nombre)
    || !!product.mpn && (importKey(item.code) === importKey(product.mpn) || importKey(item.title).includes(importKey(product.mpn))));
}
