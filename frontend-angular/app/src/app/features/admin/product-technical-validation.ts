import { ProductTechnicalFicha, TechnicalSchema } from './product-technical.models';

/** Shared by the editor and importer: unknown values are not zero or false. */
export function technicalIssues(schema:TechnicalSchema|null|undefined, ficha:ProductTechnicalFicha):string[]{
  if(!schema||schema.tipo!==ficha.tipo)return ['La ficha técnica no corresponde a la categoría seleccionada.'];
  const errors:string[]=[];
  for(const key of Object.keys(ficha.valores))if(!schema.campos.some(field=>field.key===key))errors.push(`Campo técnico desconocido: ${key}.`);
  for(const field of schema.campos){const value=ficha.valores[field.key];if(value==null||value==='')continue;
    if(field.type==='number'&&(typeof value!=='number'||!Number.isFinite(value)||value<0&&!field.custom||field.positive&&value<=0||field.integer&&!Number.isInteger(value)||Math.abs(value)>=1e12||Math.abs(value*10000-Math.round(value*10000))>0.001))errors.push(`${field.label}: número válido${field.custom?'':' no negativo'}, máximo cuatro decimales${field.integer?(field.positive?', entero positivo':field.custom?', entero':', entero no negativo'):''}.`);
    if(field.type==='boolean'&&typeof value!=='boolean')errors.push(`${field.label}: selecciona Sí, No o desconocido.`);
    if(field.type==='text'&&(typeof value!=='string'||value.length>250))errors.push(`${field.label}: máximo 250 caracteres.`);
  }
  for(const [minKey,maxKey,label] of [['frequency_min_hz','frequency_max_hz','Frecuencia'],['rpm_min','rpm_max','Velocidad'],['temperature_min_k','temperature_max_k','Temperatura de color']]){
    const min=ficha.valores[minKey],max=ficha.valores[maxKey];
    if(typeof min==='number'&&typeof max==='number'&&min>max)errors.push(`${label}: el mínimo no puede superar al máximo.`);
  }
  if(ficha.fuenteUrl?.trim())try{const url=new URL(ficha.fuenteUrl.trim());if(url.protocol!=='https:'||url.username||url.password||ficha.fuenteUrl.trim().length>250)throw new Error();}catch{errors.push('La fuente debe ser una URL HTTPS sin credenciales, de hasta 250 caracteres.');}
  return errors;
}
