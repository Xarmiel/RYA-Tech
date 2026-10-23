import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TechnicalEditor } from './technical-editor';
import { TECHNICAL_MODEL } from './technical-model';
import { TechnicalEntry } from './technical-entry';
import { AdminPage } from './admin-page';
import { AdminService } from './admin.service';
import { routes } from '../../app.routes';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';
import { PreferencesService } from '../../core/preferences/preferences.service';
import { NotificationService } from '../../core/notifications/notification.service';

describe('Phase four technical draft workflow',()=>{
  beforeEach(()=>TestBed.configureTestingModule({imports:[TechnicalEditor],providers:[{provide:SupabaseStorageService,useValue:{}}]}));
  afterEach(()=>{TestBed.resetTestingModule();vi.restoreAllMocks();});
  function open(){const fixture=TestBed.createComponent(TechnicalEditor);fixture.componentRef.setInput('isDesignMode',true);fixture.detectChanges();const editor=fixture.componentInstance;
    editor.product={name:'CPU test',sku:'CPU-TEST',brand:'Test'};return {fixture,editor};}
  function ready(editor:TechnicalEditor){editor.use='evaluate';editor.source='https://example.com/datasheet';editor.date='2020-01-01';editor.values={socket:'AM5',cores:'8',threads:'16',base_w:'65',igpu:'false'};}
  function draft(){return {schema:'rya.technical-draft.v1',verificationStatus:'verified',requestedUse:'pending',product:{name:'CPU restored',sku:'RESTORED',brand:'Test',type:'cpu',price:0,stock:0},technical:{cores:8,igpu:false},relatedRecords:{},evidence:{url:'https://example.com/spec',date:'2020-01-01',notes:'Fuente pendiente'},media:[]};}
  it('reports all missing fields rather than stopping after the first one',()=>{
    const {editor}=open();editor.use='evaluate';editor.review();expect(editor.issues()).toEqual(expect.arrayContaining(['Falta: Socket.','Falta: Núcleos.','Falta: Gráficos integrados.']));expect(editor.reviewed()).toBe('');
  });
  it('preserves false, zero and unknown values distinctly',()=>{
    const {editor}=open();ready(editor);editor.product['price']='0';editor.review();const result=JSON.parse(editor.reviewed());
    expect(result.technical.igpu).toBe(false);expect(result.technical.boost_mhz).toBeNull();expect(result.product.price).toBe(0);expect(result.verificationStatus).toBe('pending');
    const field=TECHNICAL_MODEL.types['cpu'].fields.find(f=>f.id==='cores')!;expect(TECHNICAL_MODEL.valueOf(field,'   ')).toBeNull();
  });
  it('rejects fractional counts and conflicting CPU ranges',()=>{
    const {editor}=open();ready(editor);editor.values['cores']='8.5';editor.values['base_mhz']='4000';editor.values['boost_mhz']='3000';editor.review();
    expect(editor.issues().some(v=>v.includes('entero'))).toBe(true);expect(editor.error()).toContain('frecuencia máxima');
  });
  it('rejects impossible and future evidence dates',()=>{
    const {editor}=open();ready(editor);editor.date='2025-02-30';editor.review();expect(editor.error()).toContain('fecha');
    editor.date='2999-01-01';editor.review();expect(editor.error()).toContain('futura');
  });
  it('preserves separate evidence and specification drafts per type',()=>{
    const {editor}=open();ready(editor);editor.type='board';editor.changeType();expect(editor.source).toBe('');expect(editor.values).toEqual({});
    editor.values['chipset']='B650';editor.source='https://example.com/board';editor.type='cpu';editor.changeType();
    expect(editor.values['socket']).toBe('AM5');expect(editor.source).toBe('https://example.com/datasheet');expect(editor.use).toBe('evaluate');
  });
  it('does not treat peripherals as compatible PC components',()=>{
    const {editor}=open();editor.type='peripheral';editor.changeType();expect(editor.use).toBe('catalog');editor.use='evaluate';editor.review();expect(editor.error()).toContain('catálogo');
  });
  it('keeps technical attachments local even outside design until drafts can be persisted',()=>{
    const fixture=TestBed.createComponent(TechnicalEditor);fixture.componentRef.setInput('isDesignMode',false);fixture.detectChanges();
    expect(fixture.componentInstance.media.isDesignMode()).toBe(true);
  });
  it('ignores completely blank records and flags incomplete evidence in a nonempty row',()=>{
    const {editor}=open();ready(editor);editor.addRow('memory');editor.review();expect(JSON.parse(editor.reviewed()).relatedRecords.memory).toEqual([]);
    editor.records['memory'][0]['type']='DDR5';editor.review();expect(editor.error()).toContain('Fuente HTTPS');expect(editor.error()).toContain('Velocidad');
  });
  it('keeps related records pending and never infers their database IDs',()=>{
    const {editor}=open();editor.records['cpu_board']=[{cpu_sku:'CPU',board_sku:'BOARD',source_url:'https://example.com/support'}];editor.review();
    const record=JSON.parse(editor.reviewed()).relatedRecords.cpu_board[0];expect(record.resolutionStatus).toBe('pending');expect(record.cpuId).toBeUndefined();expect(editor.warnings().some(v=>v.includes('sin vincular'))).toBe(true);
  });
  it('rejects unsafe record sources, self-relations and excessive rows',()=>{
    const {editor}=open();editor.records['cpu_board']=[{cpu_sku:'SAME',board_sku:'SAME',source_url:'http://example.com'}];editor.review();
    expect(editor.error()).toContain('productos distintos');expect(editor.error()).toContain('HTTPS');
    editor.records['memory']=Array.from({length:20},()=>({}));editor.addRow('memory');expect(editor.records['memory']).toHaveLength(20);
  });
  it('blocks broken images but allows explicit local-file metadata with a warning',()=>{
    const {editor}=open();editor.media.images.set([{id:1,url:'https://example.com/image.png',alt:'Test',previewError:'No disponible'}]);editor.review();expect(editor.error()).toContain('imágenes');
    editor.media.images.set([{id:2,url:'blob:local',alt:'Local',file:new File(['x'],'local.png',{type:'image/png'})}]);editor.review();
    expect(editor.reviewed()).not.toBe('');expect(JSON.parse(editor.reviewed()).media[0].url).toBeNull();expect(editor.warnings().some(v=>v.includes('archivos locales'))).toBe(true);
  });
  it('recovers known fields without importing a verified status or arbitrary properties',()=>{
    const {editor}=open();const imported={...draft(),technical:{cores:8,igpu:false,arbitrary:'ignored'}};
    expect(editor.restoreDraft(JSON.stringify(imported))).toBe(true);expect(editor.values['igpu']).toBe('false');expect(editor.values['arbitrary']).toBeUndefined();
    editor.review();const result=JSON.parse(editor.reviewed());expect(result.verificationStatus).toBe('pending');expect(result.product.stock).toBe(0);expect(result.technical.igpu).toBe(false);
  });
  it('rejects invalid imports atomically without replacing the current draft',()=>{
    const {editor}=open();const original=editor.product;expect(editor.restoreDraft(JSON.stringify({...draft(),media:[{url:'javascript:alert(1)'}]}))).toBe(false);
    expect(editor.product).toBe(original);expect(editor.restoreDraft(JSON.stringify({...draft(),technical:[]}))).toBe(false);expect(editor.restoreDraft('x'.repeat(1024*1024+1))).toBe(false);
  });
  it('warns when JSON recovery cannot restore file bytes',()=>{
    const {editor}=open();expect(editor.restoreDraft(JSON.stringify({...draft(),media:[{url:null,file:{name:'local.png'}}]}))).toBe(true);
    expect(editor.recoveryNotice()).toContain('1 archivo');expect(editor.media.images()).toEqual([]);
  });
  it('prompts before leaving and prevents browser unload with dirty data',()=>{
    const {editor}=open();const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);expect(editor.canLeave()).toBe(false);expect(confirm).toHaveBeenCalled();
    const event=new Event('beforeunload',{cancelable:true});editor.beforeUnload(event as BeforeUnloadEvent);expect(event.defaultPrevented).toBe(true);
    confirm.mockReturnValue(true);expect(editor.canLeave()).toBe(true);
  });
  it('does not flag an untouched form or empty type switches as unsaved data',()=>{
    const fixture=TestBed.createComponent(TechnicalEditor);fixture.detectChanges();const editor=fixture.componentInstance;
    expect(editor.isDirty()).toBe(false);editor.type='board';editor.changeType();expect(editor.isDirty()).toBe(false);
  });
  it('invalidates stale reviews after an edit',()=>{
    const {editor,fixture}=open();editor.review();expect(editor.reviewed()).not.toBe('');fixture.detectChanges();
    const input=fixture.nativeElement.querySelector('#commercial-name') as HTMLInputElement;input.value='New name';input.dispatchEvent(new Event('input',{bubbles:true}));
    expect(editor.reviewed()).toBe('');
  });
  it('focuses the validation summary after reviewing incomplete data',async()=>{
    const {editor,fixture}=open();editor.use='evaluate';editor.review();fixture.detectChanges();await fixture.whenStable();
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('.validation-summary'));
  });
  it('ignores an asynchronous import that finishes after component destruction',async()=>{
    const fixture=TestBed.createComponent(TechnicalEditor);fixture.detectChanges();const editor=fixture.componentInstance;let resolve!:(raw:string)=>void;
    const file={size:100,text:()=>new Promise<string>(r=>resolve=r)};const importing=editor.importFile({target:{files:[file],value:''}} as unknown as Event);
    expect(editor.importing()).toBe(true);expect(editor.canLeave()).toBe(false);fixture.destroy();resolve(JSON.stringify(draft()));await importing;expect(editor.product).toEqual({});
  });
});

describe('Phase four navigation protection',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideRouter(routes),
    {provide:AdminService,useValue:{getPreviewStats:()=>of({totalProductos:0}),getPreviewItems:()=>of({items:[],total:0,page:0,totalPages:0}),isModuleConnected:()=>true}},
    {provide:SupabaseStorageService,useValue:{}},{provide:PreferencesService,useValue:{theme:signal('light'),setTheme:vi.fn()}},
    {provide:NotificationService,useValue:{show:vi.fn()}}]}));
  afterEach(()=>{TestBed.resetTestingModule();vi.restoreAllMocks();});
  it('protects both reused admin sections and leaving the technical editor for the overview',async()=>{
    TestBed.inject(TechnicalEntry).open();const harness=await RouterTestingHarness.create();
    const page=await harness.navigateByUrl('/admin/diseno/carga-tecnica',AdminPage);
    const editor=(page as any).technicalEditor as TechnicalEditor;editor.product={name:'Dirty draft'};
    const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);
    await harness.navigateByUrl('/admin/diseno/catalogo');expect(TestBed.inject(Router).url).toBe('/admin/diseno/carga-tecnica');
    expect(confirm).toHaveBeenCalledTimes(1);
    await harness.navigateByUrl('/admin/diseno');expect(TestBed.inject(Router).url).toBe('/admin/diseno/carga-tecnica');
    confirm.mockReturnValue(true);await harness.navigateByUrl('/admin/diseno/catalogo');expect(TestBed.inject(Router).url).toBe('/admin/diseno/catalogo');
  });
});
