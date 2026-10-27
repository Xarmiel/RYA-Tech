import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CatalogService } from './catalog.service';

describe('Category filter API',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]}));
  afterEach(()=>TestBed.inject(HttpTestingController).verify());
  it('builds only the facets delivered by the category and keeps actual units and boolean values',()=>{
    let result:any;
    TestBed.inject(CatalogService).getFilters('','mouse').subscribe(value=>result=value);
    const request=TestBed.inject(HttpTestingController).expectOne(r=>r.url==='/api/v1/productos/filtros');
    expect(request.request.params.get('categoria')).toBe('mouse');
    request.flush({categorias:[],marcas:[],rangoPrecio:{min:0,max:0},atributos:[
      {codigo:'CATALOG_TECH_MOUSE_MAX_DPI',nombre:'Resolución máxima',tipoDato:'NUMERO',unidad:'DPI',valores:['16000','8000']},
      {codigo:'CATALOG_TECH_MOUSE_SILENT_CLICKS',nombre:'Clics silenciosos',tipoDato:'BOOLEANO',unidad:null,valores:['false','true']},
    ]});
    const dpi=result.groups.find((g:any)=>g.code==='CATALOG_TECH_MOUSE_MAX_DPI');
    expect(dpi.options.map((o:any)=>o.value)).toEqual(['8000','16000']);expect(dpi.options[0].label).toContain('DPI');
    expect(result.groups.find((g:any)=>g.code==='CATALOG_TECH_MOUSE_SILENT_CLICKS').options.map((o:any)=>o.label)).toEqual(['No','Sí']);
    expect(result.groups.filter((g:any)=>g.kind==='filter')).toHaveLength(2);
  });
  it('sends multiple selected specification values to the server without changing category',()=>{
    TestBed.inject(CatalogService).getProducts('',0,12,{__categoria:['mouse'],CATALOG_TECH_MOUSE_COLOR:['Negro','Blanco']}).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne(r=>r.url==='/api/v1/productos');
    expect(request.request.params.get('categoria')).toBe('mouse');
    expect(request.request.params.getAll('filtro.CATALOG_TECH_MOUSE_COLOR')).toEqual(['Negro','Blanco']);
    expect(request.request.params.has('q')).toBe(false);
    request.flush({content:[],page:0,size:12,totalElements:0,totalPages:0});
  });
});
