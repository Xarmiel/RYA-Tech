import {describe,it,expect} from 'vitest';
import {parseProducts} from './product-import-model';
import {ImportSnapshot,plannedValue,resolutionChanges} from './import-resolution-model';

describe('Duplicate import comparisons',()=>{
  const product=()=>parseProducts(JSON.stringify([{sku:'CPU',nombre:'CPU nuevo',marca:'AMD',categoria:'Procesadores',precio:99,stock:30,imagenes:['https://example.com/old','https://example.com/new'],fichaTecnica:{tipo:'cpu',fuenteUrl:'https://example.com/new-source',valores:{cores:8,tdp_w:65,igpu:true}}}]),'json')[0];
  const snapshot=():ImportSnapshot=>({revision:'r1',actual:{id:'1',sku:'CPU',nombre:'CPU actual',precioBase:0,categoriaSlug:'procesadores',imagenes:[{url:'https://example.com/old',principal:true}],fichaTecnica:{tipo:'cpu',fuenteUrl:'https://example.com/source',valores:{cores:6,tdp_w:0,igpu:false}}}});
  it('preserves zero, false, existing source and known technical values while filling missing keys',()=>{
    const current=snapshot(),incoming=product();incoming.fichaTecnica!.valores['threads']=12;
    expect(plannedValue(current,incoming,'completar','precioBase')).toBe(0);
    expect(plannedValue(current,incoming,'completar','fichaTecnica')).toEqual({tipo:'cpu',fuenteUrl:'https://example.com/source',valores:{cores:6,tdp_w:0,igpu:false,threads:12}});
  });
  it('merges images without duplicates and previews replacing only when explicitly chosen',()=>{
    expect(plannedValue(snapshot(),product(),'completar','imagenes')).toEqual([{url:'https://example.com/old'},{url:'https://example.com/new'}]);
    expect(resolutionChanges(snapshot(),product(),'actualizar').find(field=>field.key==='precioBase')).toMatchObject({before:'0',after:'99',changed:true});
  });
});
