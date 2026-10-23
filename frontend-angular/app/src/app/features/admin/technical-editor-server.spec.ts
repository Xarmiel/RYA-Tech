import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TechnicalEditor } from './technical-editor';
import { TechnicalDraftApi, TechnicalDraftDetail } from './technical-draft-api';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';
import { AuthService } from '../../core/auth/auth.service';

describe('Technical editor persistence',()=>{
  let api:any;
  const author='author';
  function document(){return {schema:'rya.technical-draft.v1',verificationStatus:'pending',requestedUse:'evaluate',
    product:{name:'CPU',sku:'CPU-1',brand:'Test',type:'cpu',price:0,stock:0},technical:{igpu:false,cores:null},
    relatedRecords:{},evidence:{url:null,date:null,notes:''},media:[]};}
  function detail(overrides:Partial<TechnicalDraftDetail>={}):TechnicalDraftDetail{return {id:'draft-1',version:0,state:'DRAFT',verificationStatus:'pending',
    createdBy:author,updatedBy:author,createdAt:'2026-10-22T00:00:00Z',updatedAt:'2026-10-22T00:00:00Z',document:document(),...overrides};}
  beforeEach(()=>{
    api={list:vi.fn(()=>of({content:[],page:0,totalPages:0,totalElements:0})),get:vi.fn(()=>of(detail())),
      create:vi.fn(()=>of(detail())),update:vi.fn(()=>of(detail({version:1})))};
    TestBed.configureTestingModule({imports:[TechnicalEditor],providers:[{provide:TechnicalDraftApi,useValue:api},
      {provide:SupabaseStorageService,useValue:{}},{provide:AuthService,useValue:{user:()=>({id:author})}}]});
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.restoreAllMocks();});
  function open(design=false){const fixture=TestBed.createComponent(TechnicalEditor);fixture.componentRef.setInput('isDesignMode',design);fixture.detectChanges();
    const editor=fixture.componentInstance;editor.product={name:'CPU',sku:'CPU-1',brand:'Test'};return {editor,fixture};}

  it('makes no server calls or remote uploads in design',()=>{
    const {editor}=open(true);editor.listServer();editor.loadServer('draft');editor.saveServer();
    expect(api.list).not.toHaveBeenCalled();expect(api.get).not.toHaveBeenCalled();expect(api.create).not.toHaveBeenCalled();
    expect(editor.media.isDesignMode()).toBe(true);
  });
  it('saves incomplete technical data only after the real server responds',()=>{
    const {editor}=open();editor.use='evaluate';editor.values={igpu:'false'};editor.product['price']='0';editor.product['stock']='0';
    const pending=new Subject<TechnicalDraftDetail>();api.create.mockReturnValue(pending);
    editor.saveServer();editor.saveServer();
    expect(api.create).toHaveBeenCalledTimes(1);expect(editor.serverDraft()).toBeNull();expect(editor.serverNotice()).toBe('');
    expect(editor.canLeave()).toBe(false);
    expect(api.create.mock.calls[0][0]).toMatchObject({verificationStatus:'pending',technical:{igpu:false,cores:null},product:{price:0,stock:0}});
    pending.next(detail());pending.complete();
    expect(editor.serverDraft()?.id).toBe('draft-1');expect(editor.serverNotice()).toContain('Guardado');expect(editor.isDirty()).toBe(false);
    editor.product['name']='Changed';expect(editor.isDirty()).toBe(true);
  });
  it('uses the server ID/version for a subsequent save',()=>{
    const {editor}=open();editor.saveServer();editor.product['name']='Changed';editor.saveServer();
    expect(api.create).toHaveBeenCalledTimes(1);expect(api.update).toHaveBeenCalledWith(expect.objectContaining({id:'draft-1',version:0}),expect.anything());
    expect(editor.serverDraft()?.version).toBe(1);
  });
  it('updates the visible list to the confirmed server version after saving',()=>{
    const {editor}=open();editor.saveServer();
    editor.serverRows.set([{id:'draft-1',version:0,name:'Old name',sku:'CPU-1',type:'cpu',state:'DRAFT',createdBy:author,updatedAt:'old'}]);
    editor.saveServer();
    expect(editor.serverRows()[0]).toMatchObject({version:1,name:'CPU',sku:'CPU-1'});
  });
  it('preserves a stable product link on recovery, review transition and later saves',()=>{
    const {editor}=open();const id='10000000-1000-4000-8000-000000000003';
    editor.product={};api.get.mockReturnValue(of(detail({document:{...document(),catalogProductId:id}})));
    editor.loadServer('draft-1');expect(editor.catalogProductId).toBe(id);
    editor.reviewChanged(detail({version:2,document:{...document(),catalogProductId:id}}));
    editor.product['name']='Changed';editor.saveServer();
    expect(api.update).toHaveBeenCalledWith(expect.objectContaining({version:2}),expect.objectContaining({catalogProductId:id}));
  });
  it('refuses to silently discard local file bytes when saving remotely',()=>{
    const {editor}=open();editor.media.images.set([{id:1,url:'blob:local',alt:'Local',file:new File(['x'],'local.png',{type:'image/png'})}]);
    editor.saveServer();expect(api.create).not.toHaveBeenCalled();expect(editor.serverError()).toContain('archivos locales');
    expect(editor.media.images()).toHaveLength(1);
  });
  it('preserves input on validation errors and allows a deliberate retry',()=>{
    const {editor}=open();api.create.mockReturnValueOnce(throwError(()=>({status:400,error:{message:'Validación',issues:['SKU inválido']}})));
    editor.saveServer();expect(editor.issues()).toEqual(['SKU inválido']);expect(editor.product['sku']).toBe('CPU-1');
    expect(editor.serverNotice()).toBe('');editor.saveServer();expect(editor.serverDraft()?.id).toBe('draft-1');
  });
  it('blocks resubmission after a conflict without overwriting local input',()=>{
    const {editor}=open();editor.saveServer();editor.product['name']='Local change';
    api.update.mockReturnValue(throwError(()=>({status:409})));editor.saveServer();editor.saveServer();
    expect(api.update).toHaveBeenCalledTimes(1);expect(editor.conflict()).toBe(true);expect(editor.product['name']).toBe('Local change');
    expect(editor.serverDraft()?.version).toBe(0);
  });
  it('does not retry an uncertain creation or announce it as saved',()=>{
    const {editor}=open();api.create.mockReturnValue(throwError(()=>({status:0})));editor.saveServer();editor.saveServer();
    expect(api.create).toHaveBeenCalledTimes(1);expect(editor.uncertain()).toBe(true);expect(editor.serverDraft()).toBeNull();
    expect(editor.serverNotice()).toBe('');expect(editor.product['sku']).toBe('CPU-1');
  });
  it('loads only after confirming replacement and clears the dirty baseline',()=>{
    const {editor}=open();const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);editor.loadServer('draft-1');expect(api.get).not.toHaveBeenCalled();
    confirm.mockReturnValue(true);editor.loadServer('draft-1');expect(editor.values['igpu']).toBe('false');expect(editor.product['price']).toBe('0');
    expect(editor.serverDraft()?.id).toBe('draft-1');expect(editor.isDirty()).toBe(false);expect(editor.canLeave()).toBe(true);
  });
  it('retains local data when recovery fails and exposes a retryable error',()=>{
    const {editor}=open();vi.spyOn(window,'confirm').mockReturnValue(true);api.get.mockReturnValueOnce(throwError(()=>({status:503})));
    editor.loadServer('draft-1');expect(editor.product['sku']).toBe('CPU-1');expect(editor.serverDraft()).toBeNull();expect(editor.serverError()).toContain('reinícialo');
    editor.loadServer('draft-1');expect(editor.serverDraft()?.id).toBe('draft-1');
  });
  it('keeps reviewed or other-author documents read-only',()=>{
    const {editor}=open();vi.spyOn(window,'confirm').mockReturnValue(true);api.get.mockReturnValue(of(detail({createdBy:'another-admin'})));
    editor.loadServer('draft-1');expect(editor.canEdit()).toBe(false);editor.saveServer();expect(api.update).not.toHaveBeenCalled();
    api.get.mockReturnValue(of(detail({state:'APPROVED'})));editor.loadServer('draft-1');expect(editor.canEdit()).toBe(false);
    expect(editor.serverDraft()?.verificationStatus).toBe('pending');
  });
  it('does not mark data of inactive product types as saved',()=>{
    const {editor}=open();editor.values={socket:'AM5'};editor.type='board';editor.changeType();editor.saveServer();
    expect(editor.serverNotice()).toContain('otros tipos');expect(editor.isDirty()).toBe(true);
  });
  it('lists with filters and cancels old responses',()=>{
    const {editor}=open();const old=new Subject<any>();api.list.mockReturnValueOnce(old);editor.listServer();
    editor.serverQuery='CPU';editor.serverState='DRAFT';editor.listServer(2);
    old.next({content:[{id:'stale'}],page:0,totalPages:1,totalElements:1});
    expect(api.list).toHaveBeenLastCalledWith(2,'CPU','DRAFT');expect(editor.serverRows()).toEqual([]);
  });
});
