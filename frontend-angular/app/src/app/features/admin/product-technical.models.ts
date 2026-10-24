export interface TechnicalSpecField {key:string;label:string;type:'text'|'number'|'boolean';unit:string|null;integer:boolean;positive:boolean}
export interface TechnicalSchema {tipo:string;nombre:string;campos:TechnicalSpecField[]}
export interface ProductTechnicalFicha {tipo:string;fuenteUrl?:string|null;valores:Record<string,string|number|boolean|null>;origen?:string;avisos?:string[]}
