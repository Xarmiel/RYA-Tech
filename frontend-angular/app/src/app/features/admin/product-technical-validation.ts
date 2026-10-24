import { ProductTechnicalFicha, TechnicalSchema } from './product-technical.models';

/** Shared by the editor and importer: unknown values are not zero or false. */
export function technicalIssues(schema:TechnicalSchema|null|undefined, ficha:ProductTechnicalFicha):string[]{
  if(!schema||schema.tipo!==ficha.tipo)return ['La ficha técnica no corresponde a la categoría seleccionada.'];
  const errors:string[]=[];
  for(const key of Object.keys(ficha.valores))if(!schema.campos.some(field=>field.key===key))errors.push(`Campo técnico desconocido: ${key}.`);
  for(const field of schema.campos){const value=ficha.valores[field.key];if(value==null||value==='')continue;
    if(field.type==='number'&&(typeof value!=='number'||!Number.isFinite(value)||value<0||field.positive&&value<=0||field.integer&&!Number.isInteger(value)||value>=1e12||Math.abs(value*10000-Math.round(value*10000))>0.001))errors.push(`${field.label}: número válido no negativo, máximo cuatro decimales${field.integer?', entero positivo':''}.`);
    if(field.type==='boolean'&&typeof value!=='boolean')errors.push(`${field.label}: selecciona Sí, No o desconocido.`);
    if(field.type==='text'&&(typeof value!=='string'||value.length>250))errors.push(`${field.label}: máximo 250 caracteres.`);
  }
  if(ficha.fuenteUrl?.trim())try{const url=new URL(ficha.fuenteUrl.trim());if(url.protocol!=='https:'||url.username||url.password||ficha.fuenteUrl.trim().length>250)throw new Error();}catch{errors.push('La fuente debe ser una URL HTTPS sin credenciales, de hasta 250 caracteres.');}
  return errors;
}
