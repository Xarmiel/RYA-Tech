import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CatalogReferences } from './catalog-references';
import { AdminService } from './admin.service';
import { ProductImport } from './product-import';

describe('Independent catalog references',()=>{
  let api:any, component:CatalogReferences;
  const metadata=()=>({categorias:[{id:'cat',nombre:'Memoria RAM',slug:'memoria-ram'}],marcas:[{id:'amd',nombre:'AMD',slug:'amd'}]});
  beforeEach(()=>{
    api={getReferences:vi.fn(()=>of(metadata())),getMetadata:vi.fn(()=>of(metadata())),deleteReference:vi.fn(()=>of(undefined)),restoreReference:vi.fn(()=>of(undefined)),createBrand:vi.fn(()=>of({id:'corsair',nombre:'Corsair',slug:'corsair'})),createCategory:vi.fn(()=>of({id:'ssd',nombre:'SSD',slug:'ssd'}))};
    TestBed.configureTestingModule({imports:[CatalogReferences,ProductImport],providers:[{provide:AdminService,useValue:api}]});
    component=TestBed.createComponent(CatalogReferences).componentInstance;
  });
  afterEach(()=>{vi.restoreAllMocks();TestBed.resetTestingModule();});
  it('creates a real brand independently, only after explicit submission',async()=>{
    await component.load();expect(api.createBrand).not.toHaveBeenCalled();
    component.nombre=' Corsair ';await component.create();
    expect(api.createBrand).toHaveBeenCalledWith({nombre:'Corsair'});
    expect(component.metadata().marcas.some(item=>item.id==='corsair')).toBe(true);
    expect(component.nombre).toBe('');expect(component.message()).toContain('servidor');
  });
  it('creates categories with optional parent and acronym',async()=>{
    await component.load();component.kind='categoria';component.nombre='SSD';component.parentId='cat';component.acronimo='SSD';
    await component.create();expect(api.createCategory).toHaveBeenCalledWith({nombre:'SSD',parentId:'cat',acronimo:'SSD'});
  });
  it('rejects duplicate names ignoring case, spacing and accents',async()=>{
    await component.load();component.nombre=' ámd ';await component.create();expect(api.createBrand).not.toHaveBeenCalled();
    expect(component.error()).toContain('Ya existe');
  });
  it('requires a successful metadata load',async()=>{
    api.getReferences.mockReturnValue(throwError(()=>({status:503})));await component.load();component.nombre='Corsair';await component.create();
    expect(api.createBrand).not.toHaveBeenCalled();expect(component.loaded()).toBe(false);
  });
  it('blocks missing parent IDs and invalid field lengths',async()=>{
    await component.load();component.kind='categoria';component.nombre='SSD';component.parentId='missing';await component.create();expect(api.createCategory).not.toHaveBeenCalled();
    component.parentId='';component.acronimo='x'.repeat(21);await component.create();expect(component.error()).toContain('20');
  });
  it('preserves input after server rejection and does not invent a successful row',async()=>{
    await component.load();api.createBrand.mockReturnValue(throwError(()=>({status:400,error:{message:'Nombre duplicado'}})));
    component.nombre='Corsair';await component.create();expect(component.nombre).toBe('Corsair');expect(component.metadata().marcas).toHaveLength(1);expect(component.message()).toBe('');
  });
  it('locks after an uncertain response and permits read-only reconciliation',async()=>{
    await component.load();api.createBrand.mockReturnValue(throwError(()=>({status:0})));component.nombre='Corsair';await component.create();await component.create();
    expect(component.uncertain()).toBe(true);expect(api.createBrand).toHaveBeenCalledTimes(1);await component.load();expect(api.getReferences).toHaveBeenCalledTimes(2);
  });
  it('prevents repeated submissions during a request',async()=>{
    await component.load();const pending=new Subject<any>();api.createBrand.mockReturnValue(pending);component.nombre='Corsair';
    const first=component.create();await component.create();expect(api.createBrand).toHaveBeenCalledTimes(1);
    pending.next({id:'corsair',nombre:'Corsair',slug:'corsair'});pending.complete();await first;expect(component.busy()).toBe(false);
  });
  it('preserves importer source, rows and manual selections when references change',()=>{
    const importer=TestBed.createComponent(ProductImport).componentInstance;
    importer.source='archivo original';
    importer.rows.set([{product:{nombre:'RAM',sku:'R1',marca:'Corsair',categoria:'Memoria RAM',mpn:'',precio:100,stockInicial:30,descripcion:'',imagenes:[],errors:[],warnings:[]},categoriaId:'cat',marcaId:'',selected:true,state:'ready',message:''}]);
    importer.confirmed=true;
    importer.referencesChanged({...metadata(),marcas:[...metadata().marcas,{id:'corsair',nombre:'Corsair',slug:'corsair'}]});
    expect(importer.source).toBe('archivo original');expect(importer.rows()[0]).toMatchObject({categoriaId:'cat',marcaId:'corsair',selected:true});expect(importer.confirmed).toBe(false);
  });
  it('renders creation and read-only lists with accessible labels',async()=>{
    const fixture=TestBed.createComponent(CatalogReferences);fixture.detectChanges();await vi.waitFor(()=>expect(fixture.componentInstance.loaded()).toBe(true));await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('AMD');expect(fixture.nativeElement.querySelector('input[name="nombre"]').required).toBe(true);
  });
  it('supports a long slug separately from a short optional acronym',async()=>{
    await component.load();component.kind='categoria';component.nombre='Audífonos con micrófono';component.slug='audifonos-con-microfono';component.acronimo='AUD-MIC';
    await component.create();expect(api.createCategory).toHaveBeenCalledWith(expect.objectContaining({slug:'audifonos-con-microfono',acronimo:'AUD-MIC'}));
  });
  it('does not delete when the confirmation is cancelled',async()=>{
    await component.load();vi.spyOn(window,'confirm').mockReturnValue(false);await component.changeDeleted('marca',component.metadata().marcas[0]);expect(api.deleteReference).not.toHaveBeenCalled();
  });
  it('removes an unused brand only after server success and updates active selectors',async()=>{
    await component.load();vi.spyOn(window,'confirm').mockReturnValue(true);const changed=vi.fn();component.changed.subscribe(changed);
    await component.changeDeleted('marca',component.metadata().marcas[0]);expect(api.deleteReference).toHaveBeenCalledWith('marca','amd');expect(component.metadata().marcas).toEqual([]);expect(changed).toHaveBeenCalledWith(expect.objectContaining({marcas:[]}));
  });
  it('preserves a category if the server rejects deletion because it has products',async()=>{
    await component.load();vi.spyOn(window,'confirm').mockReturnValue(true);api.deleteReference.mockReturnValue(throwError(()=>({status:400,error:{message:'Tiene productos asociados'}})));
    await component.changeDeleted('categoria',component.metadata().categorias[0]);expect(component.metadata().categorias).toHaveLength(1);expect(component.error()).toContain('productos');
  });
  it('restores a deleted row and never includes deleted rows in selector events',async()=>{
    component.showDeleted=true;api.getReferences.mockReturnValue(of({...metadata(),marcas:[{id:'amd',nombre:'AMD',slug:'amd',eliminada:true}]}));
    const changed=vi.fn();component.changed.subscribe(changed);await component.load();expect(changed).toHaveBeenLastCalledWith(expect.objectContaining({marcas:[]}));
    vi.spyOn(window,'confirm').mockReturnValue(true);await component.changeDeleted('marca',component.metadata().marcas[0]);expect(api.restoreReference).toHaveBeenCalledWith('marca','amd');expect(changed.mock.lastCall?.[0].marcas[0].eliminada).toBe(false);
  });
  it('locks deletion on unknown response without pretending success',async()=>{
    await component.load();vi.spyOn(window,'confirm').mockReturnValue(true);api.deleteReference.mockReturnValue(throwError(()=>({status:0})));
    const row=component.metadata().marcas[0];await component.changeDeleted('marca',row);await component.changeDeleted('marca',row);expect(api.deleteReference).toHaveBeenCalledTimes(1);expect(component.metadata().marcas).toHaveLength(1);expect(component.uncertain()).toBe(true);
  });
});
