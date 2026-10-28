import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductImport } from './product-import';
import { AdminService } from './admin.service';
import { findDuplicate, parseProducts, readCsv } from './product-import-model';

const sourceRow = () => ({ nombre: 'RAM 16 GB', sku: 'RAM-1', mpn: 'CMK16GX4M2B3200C16', marca: 'Corsair', categoria: 'Memoria RAM', precio: 189, stock: 30, descripcion: 'Kit 2 x 8 GB', imagenes: ['https://example.com/ram.webp'] });
const batch = () => JSON.stringify([sourceRow()]);

describe('Product import parsing', () => {
  it('recognizes explicit partial technical updates and rejects mismatched field lists',()=>{
    const row={nombre:'Mouse',sku:'MOUSE-1',accion:'actualizar-ficha',camposFicha:['variant_notes','internal_notes'],fichaTecnica:{tipo:'mouse',valores:{variant_notes:null,internal_notes:'Revisar modelo'}}};
    const parsed=parseProducts(JSON.stringify([row]),'json')[0];expect(parsed.errors).toEqual([]);expect(parsed.camposFicha).toEqual(row.camposFicha);
    for(const camposFicha of [[],['variant_notes'],['variant_notes','variant_notes'],['unknown']])expect(parseProducts(JSON.stringify([{...row,camposFicha}]),'json')[0].errors.join()).toContain('camposFicha');
  });
  it('parses image-only actions without requiring price, stock, brand, category or technical data',()=>{
    const [row]=parseProducts(JSON.stringify({modo:'agregar-imagenes',productos:[{accion:'agregar-imagenes',sku:'CPU-1',nombre:'CPU',imagenes:['https://example.com/cpu.webp']}]}),'json');
    expect(row.errors).toEqual([]);expect(row.accion).toBe('agregar-imagenes');expect(row.fichaTecnica).toBeUndefined();
  });
  it('does not silently mix specifications into an image-only action',()=>{
    const [row]=parseProducts(JSON.stringify([{accion:'agregar-imagenes',sku:'CPU-1',nombre:'CPU',imagenes:['https://example.com/cpu.webp'],fichaTecnica:{tipo:'cpu',valores:{cores:6}}}]),'json');
    expect(row.errors.join()).toContain('acciones separadas');
  });
  it('preserves the explicit technical document and rejects a consultation backup',()=>{
    const fichaTecnica={tipo:'cpu',fuenteUrl:'https://example.com/cpu',valores:{cores:6,igpu:false,max_w:null}};
    const result=parseProducts(JSON.stringify([{...sourceRow(),fichaTecnica}]),'json')[0];
    expect(result.fichaTecnica).toEqual(fichaTecnica);expect(result.errors).toEqual([]);
    expect(()=>parseProducts(JSON.stringify({modo:'respaldo-no-importar',productos:[sourceRow()]}),'json')).toThrow('respaldo');
  });
  it('does not require or invent commercial quantities for a technical-only update',()=>{
    const [product]=parseProducts(JSON.stringify([{accion:'actualizar-ficha',nombre:'CPU anterior',sku:'CPU-1',categoria:'Procesadores',fichaTecnica:{tipo:'cpu',valores:{cores:6}}}]),'json');
    expect(product.errors).toEqual([]);expect(product.warnings).not.toContain('Sin imágenes.');expect(product.accion).toBe('actualizar-ficha');
  });
  it.each([null,'text',[],{tipo:'cpu',valores:[]},{tipo:'cpu',valores:{cores:{value:6}}},{tipo:'cpu',fuenteUrl:42,valores:{}}])('rejects malformed technical objects: %j',fichaTecnica=>{
    expect(parseProducts(JSON.stringify([{...sourceRow(),fichaTecnica}]),'json')[0].errors.join()).toContain('Ficha técnica');
  });
  it('reads our prepared envelope without treating technical data as commercial fields', () => {
    const row = sourceRow();
    const [result] = parseProducts(JSON.stringify({productos:[{...row,sku:undefined,precio:undefined,stock:undefined,skuPropuesto:'RYA-1',precioDemoPEN:189,stockInicialDemo:30, categoriaPropuesta:'Memoria RAM',imagenes:undefined,imagenesCandidatas:[{url:'https://example.com/a.webp'}],especificacionesVerificadas:{socket:'AM4'}}]}), 'json');
    expect(result).toMatchObject({sku:'RYA-1',precio:189,stockInicial:30,imagenes:['https://example.com/a.webp'],errors:[]});
    expect(result.warnings.join()).toContain('NO se importan');
  });
  it('parses CSV BOM, CRLF, commas, escaped quotes and multiline descriptions', () => {
    const csv = '\uFEFFsku,nombre,precio,stock,descripcion\r\nRAM-1,"RAM, kit",189,30,"Primera línea\n""segunda"""';
    const result = parseProducts(csv,'csv')[0];
    expect(result.nombre).toBe('RAM, kit'); expect(result.descripcion).toBe('Primera línea\n"segunda"');
    expect(result.errors).toEqual([]);
  });
  it.each(['sku,nombre\na,"sin cerrar', 'sku,nombre\na,"b"c', 'sku,sku\na,b', 'sku,nombre\na,b,c'])('rejects malformed CSV: %s', csv => expect(() => readCsv(csv)).toThrow());
  it.each([null, '', true, -1, 'no', 1.234])('rejects invalid explicit price %s', precio => {
    expect(parseProducts(JSON.stringify([{...sourceRow(),precio}]),'json')[0].errors.join()).toContain('Precio');
  });
  it.each([undefined, null, '', 1.5, -1, 2147483648])('rejects invalid or missing stock %s', stock => {
    expect(parseProducts(JSON.stringify([{...sourceRow(),stock}]),'json')[0].errors.join()).toContain('Stock');
  });
  it('allows explicit zero price and zero stock', () => expect(parseProducts(JSON.stringify([{...sourceRow(),precio:0,stock:0}]),'json')[0].errors).toEqual([]));
  it('rejects credentials, javascript and non-HTTPS URLs', () => {
    for (const url of ['http://a.test/a','javascript:alert(1)','https://user:secret@a.test/a'])
      expect(parseProducts(JSON.stringify([{...sourceRow(),imagenes:[url]}]),'json')[0].errors.join()).toContain('HTTPS');
  });
  it('limits gallery size and rejects duplicate image URLs', () => {
    expect(parseProducts(JSON.stringify([{...sourceRow(),imagenes:Array(9).fill('https://a.test/a')}]),'json')[0].errors.join()).toContain('ocho');
    expect(parseProducts(JSON.stringify([{...sourceRow(),imagenes:['https://a.test/a','https://a.test/a']}]),'json')[0].errors.join()).toContain('repetidas');
  });
  it('detects duplicate names, SKU and manufacturer part numbers within a batch', () => {
    const results = parseProducts(JSON.stringify([sourceRow(),{...sourceRow(),sku:'ram-1',nombre:'Otro'}]),'json');
    expect(results.every(row=>row.errors.some(error=>error.includes('repetido')))).toBe(true);
  });
  it('detects an existing MPN even when internal SKUs and display names differ', () => {
    const row = parseProducts(batch(),'json')[0];
    expect(findDuplicate(row,[{code:'CMK16GX4M2B3200C16',title:'Vengeance LPX'}])).toBeTruthy();
  });
  it('limits file bytes and product count', () => {
    expect(()=>parseProducts(' '.repeat(2*1024*1024+1),'json')).toThrow('2 MB');
    expect(()=>parseProducts('[]','json')).toThrow('100');
    expect(()=>parseProducts(JSON.stringify(Array(101).fill(sourceRow())),'json')).toThrow('100');
  });
});

describe('Product import workflow (no remote writes)', () => {
  let api: any, component: ProductImport;
  const page = (items: any[] = [], totalPages = 1) => ({items,totalPages,total:items.length,page:0,pageSize:50});
  beforeEach(() => {
    api = {
      getMetadata: vi.fn(()=>of({categorias:[{id:'cat',nombre:'Memoria RAM',slug:'memoria-ram'}],marcas:[{id:'brand',nombre:'Corsair',slug:'corsair'}]})),
      getItems: vi.fn(()=>of(page())),
      saveItem: vi.fn(()=>of({row:{id:'saved',code:'RAM-1',title:'RAM 16 GB'},success:true})),
      updateProductTechnical:vi.fn(()=>of({id:'existing'})),
      addProductImages:vi.fn(()=>of({id:'existing'})),
      getImportComparison:vi.fn(()=>of({revision:'revision-1',actual:{id:'existing',sku:'RAM-1',nombre:'RAM actual',categoriaSlug:'memoria-ram',marcaSlug:'corsair',precioBase:100,descripcion:null,imagenes:[]}})),
      resolveImport:vi.fn(()=>of({revision:'revision-2',actual:{id:'existing',sku:'RAM-1'}})),
    };
    TestBed.configureTestingModule({imports:[ProductImport],providers:[{provide:AdminService,useValue:api}]});
    component = TestBed.createComponent(ProductImport).componentInstance;
    component.source = batch();
  });
  afterEach(()=>TestBed.resetTestingModule());
  it('sends the partial field allowlist without commercial or inventory updates',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'MOUSE-1',title:'Mouse'}])));
    api.getMetadata.mockReturnValue(of({categorias:[{id:'cat',slug:'mouse',nombre:'Mouse',fichaEsquema:{tipo:'mouse',nombre:'Mouse',campos:[{key:'internal_notes',label:'Notas internas',type:'text',unit:null,integer:false,positive:false}]}}],marcas:[]}));
    component.source=JSON.stringify([{nombre:'Mouse',sku:'MOUSE-1',categoria:'Mouse',accion:'actualizar-ficha',camposFicha:['internal_notes'],fichaTecnica:{tipo:'mouse',valores:{internal_notes:'Revisar modelo'}}}]);
    await component.preview();component.confirmed=true;await component.save();
    expect(api.updateProductTechnical).toHaveBeenCalledWith('existing','MOUSE-1',expect.objectContaining({valores:{internal_notes:'Revisar modelo'}}),['internal_notes']);expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('keeps exact duplicates by default, and completes them only after an explicit comparison',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'RAM-1',title:'RAM actual'}])));
    await component.preview();const row=component.rows()[0];
    expect(row.resolution).toBe('conservar');expect(row.selected).toBe(false);expect(api.getImportComparison).not.toHaveBeenCalled();
    await component.chooseResolution(row,'completar');expect(component.ready(row)).toBe(true);component.confirmed=true;await component.save();
    expect(api.resolveImport).toHaveBeenCalledWith('existing',expect.objectContaining({sku:'RAM-1',revision:'revision-1',modo:'completar',campos:[]}));
    const payload=api.resolveImport.mock.calls[0][1];expect(payload.datos).not.toHaveProperty('stockInicial');expect(payload.datos).not.toHaveProperty('estado');
    expect(api.saveItem).not.toHaveBeenCalled();expect(row.state).toBe('saved');
  });
  it('requires choosing replacement fields and sends only their allowlist',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'RAM-1',title:'RAM actual'}])));await component.preview();const row=component.rows()[0];
    await component.chooseResolution(row,'actualizar');expect(component.ready(row)).toBe(false);
    component.toggleResolutionField(row,'nombre',true);expect(component.ready(row)).toBe(true);component.confirmed=true;await component.save();
    expect(api.resolveImport.mock.calls[0][1].campos).toEqual(['nombre']);expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('never resolves products matched only by name or a normalized different SKU',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'other',code:'ram1',title:'RAM 16 GB'}])));await component.preview();
    expect(component.rows()[0].duplicateId).toBeUndefined();await component.chooseResolution(component.rows()[0],'completar');
    expect(api.getImportComparison).not.toHaveBeenCalled();expect(api.resolveImport).not.toHaveBeenCalled();
  });
  it('prepares a bulk decision without writing and can return all duplicates to keep',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'RAM-1',title:'RAM actual'}])));await component.preview();
    component.bulkMode.set('completar');await component.applyResolutionToAll();expect(component.rows()[0].selected).toBe(true);
    expect(api.resolveImport).not.toHaveBeenCalled();component.bulkMode.set('conservar');await component.applyResolutionToAll();expect(component.rows()[0].selected).toBe(false);
  });
  it('reports concurrent changes and prevents reapplying stale resolutions',async()=>{
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'RAM-1',title:'RAM actual'}])));await component.preview();const row=component.rows()[0];await component.chooseResolution(row,'completar');
    api.resolveImport.mockReturnValue(throwError(()=>({status:409,error:{message:'El producto cambió después de la comparación.'}})));component.confirmed=true;await component.save();await component.save();
    expect(api.resolveImport).toHaveBeenCalledTimes(1);expect(row.state).toBe('error');expect(component.report().some(item=>item.issues.join().includes('cambió'))).toBe(true);
  });
  it('reports every missing brand and category by product without writing, then resolves newly created references', async () => {
    component.source=JSON.stringify([sourceRow(),{...sourceRow(),nombre:'Otra RAM',sku:'RAM-2',mpn:'OTRO',marca:'Razer',categoria:'Otra categoría'}]);
    await component.preview();
    expect(component.report()).toEqual([expect.objectContaining({sku:'RAM-2',issues:expect.arrayContaining([
      expect.stringContaining('marca registrada: Razer'),expect.stringContaining('categoría registrada: Otra categoría')
    ])})]);
    component.confirmed=true;await component.save();expect(api.saveItem).not.toHaveBeenCalled();
    component.referencesChanged({categorias:[...component.metadata().categorias,{id:'new-cat',nombre:'Otra categoría',slug:'otra'}],marcas:[...component.metadata().marcas,{id:'new-brand',nombre:'Razer',slug:'razer'}]});
    expect(component.requirements(component.rows()[1])).toEqual([]);
    expect(component.confirmed).toBe(false);
  });
  it('allows explicitly excluding a blocked row and saving the valid subset', async () => {
    component.source=JSON.stringify([sourceRow(),{...sourceRow(),nombre:'Otra RAM',sku:'RAM-2',mpn:'OTRO',marca:'Sin registrar'}]);
    await component.preview();component.rows()[1].selected=false;component.confirmed=true;await component.save();
    expect(api.saveItem).toHaveBeenCalledTimes(1);expect(component.rows()[0].state).toBe('saved');expect(component.rows()[1].state).toBe('ready');
  });
  it('opens a modal for invalid file data and missing references, and closes it with Escape', async () => {
    const fixture=TestBed.createComponent(ProductImport), c=fixture.componentInstance;
    c.open.set(true);fixture.detectChanges();
    c.source=JSON.stringify([{...sourceRow(),marca:'Razer',precio:-1}]);await c.preview();fixture.detectChanges();
    const dialog:HTMLDialogElement=fixture.nativeElement.querySelector('dialog');
    expect(dialog.open).toBe(true);expect(dialog.textContent).toContain('Razer');expect(dialog.textContent).toContain('Precio');expect(dialog.textContent).toContain('RAM-1');
    dialog.dispatchEvent(new Event('cancel',{cancelable:true}));expect(dialog.open).toBe(false);
    c.showIssues();c.resolveReferences();expect(c.referencesOpen()).toBe(true);expect(dialog.open).toBe(false);
  });
  it('reports invalid JSON and server field errors with the affected SKU', async () => {
    component.source='{';await component.preview();expect(component.report()[0].name).toBe('Revisión del lote');
    component.source=batch();await component.preview();component.confirmed=true;
    api.saveItem.mockReturnValue(throwError(()=>({status:400,error:{message:'Datos inválidos',validationErrors:{slug:'Ya está registrado'}}})));
    await component.save();expect(component.report().find(item=>item.sku==='RAM-1')?.issues.join()).toContain('slug: Ya está registrado');
  });
  it('performs no requests until preview; preview only reads', async () => {
    expect(api.getItems).not.toHaveBeenCalled(); await component.preview();
    expect(component.rows()[0].categoriaId).toBe('cat'); expect(component.rows()[0].marcaId).toBe('brand');
    expect(api.saveItem).not.toHaveBeenCalled(); expect(component.canSave()).toBe(false);
  });
  it('adds only images to the exact SKU with no arbitrary metadata and no commercial save',async()=>{
    api.getMetadata.mockReturnValue(of({categorias:[],marcas:[]}));api.getItems.mockReturnValue(of(page([{id:'existing',code:'CPU-1',title:'CPU'}])));
    component.source=JSON.stringify([{accion:'agregar-imagenes',nombre:'CPU',sku:'CPU-1',imagenes:['https://example.com/cpu.webp']}]);await component.preview();component.confirmed=true;await component.save();
    expect(api.addProductImages).toHaveBeenCalledWith('existing','CPU-1',['https://example.com/cpu.webp']);expect(api.saveItem).not.toHaveBeenCalled();expect(api.updateProductTechnical).not.toHaveBeenCalled();
  });
  it('never creates a missing product from an image-only row',async()=>{
    component.source=JSON.stringify([{accion:'agregar-imagenes',nombre:'CPU',sku:'MISSING',imagenes:['https://example.com/cpu.webp']}]);await component.preview();component.confirmed=true;await component.save();
    expect(component.rows()[0].state).toBe('error');expect(api.addProductImages).not.toHaveBeenCalled();expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('forces draft state and preserves image ordering, explicit price and stock', async () => {
    await component.preview(); component.confirmed=true; await component.save();
    expect(api.saveItem).toHaveBeenCalledWith('catalogo',expect.objectContaining({estado:'draft',precio:189,stockInicial:30,categoriaId:'cat',marcaId:'brand',imagenes:[expect.objectContaining({principal:true,posicion:0})]}),false);
    expect(component.rows()[0].state).toBe('saved'); expect(component.canSave()).toBe(false);
  });
  it('validates the category technical schema and sends typed data in the real payload',async()=>{
    const fichaTecnica={tipo:'ram',fuenteUrl:'https://example.com/specs',valores:{modules:2,speed_mts:3200}};
    api.getMetadata.mockReturnValue(of({categorias:[{id:'cat',nombre:'Memoria RAM',slug:'memoria-ram',fichaEsquema:{tipo:'ram',nombre:'RAM',campos:[
      {key:'modules',label:'Módulos',type:'number',unit:null,integer:true,positive:true},
      {key:'speed_mts',label:'Velocidad',type:'number',unit:'MT/s',integer:false,positive:false}
    ]}}],marcas:[{id:'brand',nombre:'Corsair',slug:'corsair'}]}));
    component.source=JSON.stringify([{...sourceRow(),fichaTecnica}]);await component.preview();component.confirmed=true;await component.save();
    expect(api.saveItem).toHaveBeenCalledWith('catalogo',expect.objectContaining({fichaTecnica}),false);
  });
  it('blocks missing or mismatched schemas before writes instead of discarding the specifications',async()=>{
    component.source=JSON.stringify([{...sourceRow(),fichaTecnica:{tipo:'cpu',valores:{cores:6}}}]);await component.preview();component.confirmed=true;
    expect(component.technicalErrors(component.rows()[0]).join()).toContain('categoría');await component.save();expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('updates only a technical document with explicit action and exact SKU, never a commercial save',async()=>{
    const fichaTecnica={tipo:'ram',valores:{modules:2}};
    api.getMetadata.mockReturnValue(of({categorias:[{id:'cat',nombre:'Memoria RAM',slug:'memoria-ram',fichaEsquema:{tipo:'ram',nombre:'RAM',campos:[{key:'modules',label:'Módulos',type:'number',unit:null,integer:true,positive:true}]}}],marcas:[{id:'brand',nombre:'Corsair',slug:'corsair'}]}));
    api.getItems.mockReturnValue(of(page([{id:'existing',code:'RAM-1',title:'RAM 16 GB'}])));
    component.source=JSON.stringify([{...sourceRow(),accion:'actualizar-ficha',fichaTecnica}]);await component.preview();component.confirmed=true;await component.save();
    expect(api.updateProductTechnical).toHaveBeenCalledWith('existing','RAM-1',{...fichaTecnica,fuenteUrl:null});expect(api.saveItem).not.toHaveBeenCalled();expect(component.rows()[0].state).toBe('saved');
  });
  it('never turns an update into a create or selects a similar product name',async()=>{
    component.source=JSON.stringify([{...sourceRow(),accion:'actualizar-ficha',fichaTecnica:{tipo:'ram',valores:{}}}]);
    api.getItems.mockReturnValue(of(page([{id:'other',code:'OTHER',title:'RAM 16 GB'}])));await component.preview();component.confirmed=true;await component.save();
    expect(component.rows()[0].state).toBe('error');expect(api.saveItem).not.toHaveBeenCalled();expect(api.updateProductTechnical).not.toHaveBeenCalled();
  });
  it('never assigns an arbitrary category or brand when a name does not resolve', async () => {
    component.source = JSON.stringify([{...sourceRow(),categoria:'RAM desconocida',marca:'Otra'}]);
    await component.preview(); component.confirmed=true; await component.save();
    expect(component.rows()[0]).toMatchObject({categoriaId:'',marcaId:''}); expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('fetches every catalog page and blocks existing MPN', async () => {
    api.getItems.mockReturnValueOnce(of(page([],2))).mockReturnValueOnce(of(page([{code:sourceRow().mpn,title:'RAM existente'}],2)));
    await component.preview(); expect(api.getItems).toHaveBeenCalledTimes(2);
    expect(component.rows()[0].state).toBe('duplicate');
    component.confirmed=true; await component.save(); expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('rechecks before writes and does not overwrite a product added since preview', async () => {
    await component.preview(); api.getItems.mockReturnValue(of(page([{code:'RAM-1',title:'Creado después'}])));
    component.confirmed=true; await component.save(); expect(api.saveItem).not.toHaveBeenCalled(); expect(component.rows()[0].state).toBe('duplicate');
  });
  it('fails closed when catalog preflight fails', async () => {
    api.getItems.mockReturnValue(throwError(()=>({status:503}))); await component.preview();
    expect(component.rows()).toEqual([]); expect(component.error()).toBeTruthy(); expect(api.saveItem).not.toHaveBeenCalled();
  });
  it('locks the batch after uncertain response and never retries automatically', async () => {
    await component.preview(); api.saveItem.mockReturnValue(throwError(()=>({status:0})));
    component.confirmed=true; await component.save(); await component.save(); await component.preview();
    expect(api.saveItem).toHaveBeenCalledTimes(1); expect(component.uncertain()).toBe(true); expect(component.rows()[0].state).toBe('uncertain');
  });
  it('stops on rejection and keeps earlier successful rows', async () => {
    component.source=JSON.stringify([sourceRow(),{...sourceRow(),sku:'RAM-2',nombre:'Otra RAM',mpn:'OTHER'}]);
    await component.preview(); api.saveItem.mockReturnValueOnce(of({row:{code:'RAM-1',title:'RAM 16 GB'}})).mockReturnValueOnce(throwError(()=>({status:400,error:{message:'Rechazado'}})));
    component.confirmed=true; await component.save();
    expect(component.rows().map(row=>row.state)).toEqual(['saved','error']); expect(component.uncertain()).toBe(false);
  });
  it('prevents double submits while waiting for the server', async () => {
    const pending = new Subject<any>(); await component.preview(); api.saveItem.mockReturnValue(pending);
    component.confirmed=true; const first=component.save(); await Promise.resolve(); await component.save();
    await vi.waitFor(()=>expect(api.saveItem).toHaveBeenCalledTimes(1)); expect(component.busy()).toBe(true);
    pending.next({row:{code:'RAM-1',title:'RAM'}});pending.complete(); await first;
    expect(component.busy()).toBe(false);
  });
  it('renders an accessible preview with labels and disabled save until confirmation', async () => {
    const fixture=TestBed.createComponent(ProductImport); const c=fixture.componentInstance;
    c.open.set(true);c.source=batch();await c.preview();fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('caption').textContent).toContain('Revisión');
    expect(fixture.nativeElement.querySelector('select[id^="import-category"]').value).toBe('cat');
    expect([...fixture.nativeElement.querySelectorAll('button')].find((button:any)=>button.textContent.includes('sin publicar'))).toHaveProperty('disabled',true);
  });
  it('blocks a category removed between preview and confirmation', async () => {
    await component.preview();api.getMetadata.mockReturnValue(of({categorias:[],marcas:[{id:'brand',nombre:'Corsair',slug:'corsair'}]}));
    component.confirmed=true;await component.save();
    expect(api.saveItem).not.toHaveBeenCalled();expect(component.error()).toContain('dejó de estar disponible');
  });
});
