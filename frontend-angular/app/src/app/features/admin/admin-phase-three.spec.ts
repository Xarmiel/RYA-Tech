import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Subject, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DraftImage, ProductMedia } from './product-media';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';
import { AuthService } from '../../core/auth/auth.service';

describe('Phase three gallery safety',()=>{
  let storage:any;
  let gallery:ProductMedia;
  const file=()=>new File(['test'],'product.png',{type:'image/png',lastModified:1});
  const item=():DraftImage=>({id:1,url:'blob:preview',alt:'Producto',file:file()});
  beforeEach(()=>{
    storage={upload:vi.fn(()=>of({progress:100,path:'productos/temp.png',url:'https://example.com/temp.png'})),
      queueCleanup:vi.fn(),cleanupFailures:signal([]),retryCleanup:vi.fn()};
    TestBed.configureTestingModule({providers:[{provide:SupabaseStorageService,useValue:storage}]});
    gallery=TestBed.runInInjectionContext(()=>new ProductMedia());
    vi.spyOn(URL,'createObjectURL').mockReturnValue('blob:decoded');vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{});
  });
  afterEach(()=>{gallery.ngOnDestroy();TestBed.resetTestingModule();vi.restoreAllMocks();vi.unstubAllGlobals();});
  it('blocks broken external images and restores saving after successful loading',()=>{
    gallery.url='https://example.com/product.png';gallery.addUrl();const image=gallery.images()[0];
    gallery.onImageLoadError(image);expect(gallery.hasPendingOrFailedUploads()).toBe(true);expect(gallery.getPayload()).toEqual([]);
    gallery.onImageLoaded(image);expect(gallery.hasPendingOrFailedUploads()).toBe(false);expect(gallery.getPayload()).toHaveLength(1);
  });
  it('retries failed previews and blocks saving while checking',async()=>{
    gallery.url='https://example.com/product.png';gallery.addUrl();const image=gallery.images()[0];gallery.onImageLoadError(image);
    let resolve!:()=>void;vi.stubGlobal('Image',class{src='';decode(){return new Promise<void>(r=>resolve=r);}});
    const checking=gallery.retryPreview(image);expect(gallery.hasPendingUploads()).toBe(true);resolve();await checking;
    expect(image.previewError).toBeUndefined();expect(gallery.hasPendingOrFailedUploads()).toBe(false);
  });
  it('cancels a removed upload, queues only its temporary path and ignores late results',()=>{
    const upload=new Subject<any>();storage.upload.mockReturnValue(upload);const image=item();gallery.images.set([image]);gallery.startUpload(image);
    upload.next({progress:30,path:'productos/temp.png'});gallery.remove(image);
    expect(upload.observed).toBe(false);expect(storage.queueCleanup).toHaveBeenCalledWith(['productos/temp.png']);
    upload.next({url:'https://example.com/late.png'});expect(gallery.images()).toEqual([]);
  });
  it('never deletes previously persisted gallery images',()=>{
    gallery.setImages([{url:'https://example.com/storage/v1/object/public/productos/productos/existing.png'}]);
    gallery.remove(gallery.images()[0]);expect(storage.queueCleanup).not.toHaveBeenCalled();
  });
  it('cleans uncommitted uploads when destroyed',()=>{
    const image=item();gallery.images.set([image]);gallery.startUpload(image);gallery.ngOnDestroy();
    expect(storage.queueCleanup).toHaveBeenCalledWith(['productos/temp.png']);
  });
  it('does not clean committed uploads when destroyed',()=>{
    const image=item();gallery.images.set([image]);gallery.startUpload(image);gallery.commitStagedUploads();gallery.ngOnDestroy();
    expect(storage.queueCleanup).not.toHaveBeenCalled();
  });
  it('preserves files when a save result is uncertain, including after another failed retry',()=>{
    const image=item();gallery.images.set([image]);gallery.startUpload(image);gallery.beginPersistenceAttempt();
    gallery.persistenceFailed(0);gallery.persistenceFailed(400);gallery.cleanupStagedUploads();gallery.ngOnDestroy();
    expect(gallery.uncertainSave()).toBe(true);expect(storage.queueCleanup).not.toHaveBeenCalled();
  });
  it('cleans temporary files after a definite rejected save',()=>{
    const image=item();gallery.images.set([image]);gallery.startUpload(image);gallery.beginPersistenceAttempt();
    gallery.persistenceFailed(400);gallery.cleanupStagedUploads();expect(storage.queueCleanup).toHaveBeenCalledWith(['productos/temp.png']);
  });
  it('does not allow repeated upload starts while a request is active',()=>{
    const upload=new Subject<any>();storage.upload.mockReturnValue(upload);const image=item();gallery.images.set([image]);
    gallery.startUpload(image);gallery.startUpload(image);gallery.retryUpload(image);expect(storage.upload).toHaveBeenCalledTimes(1);
  });
  it('retries uploads with a fresh path instead of overwriting the failed path',()=>{
    storage.upload.mockReturnValueOnce(of({progress:0,path:'productos/failed.png',error:'Falló'}));
    const image=item();gallery.images.set([image]);gallery.startUpload(image);gallery.retryUpload(image);
    expect(storage.queueCleanup).toHaveBeenCalledWith(['productos/failed.png']);expect(storage.upload.mock.calls[1]).toHaveLength(1);
    expect(image.error).toBeUndefined();
  });
  it('rejects out-of-range ordering without corrupting the array',()=>{
    gallery.url='https://example.com/1.png';gallery.addUrl();gallery.move(0,-1);gallery.move(5,1);gallery.move(0,2);
    expect(gallery.images()).toHaveLength(1);expect(gallery.images()[0].url).toContain('/1.png');
  });
  it('flags invalid and duplicate restored URLs rather than silently accepting them',()=>{
    gallery.setImages([{url:'http://example.com/x.png'},{url:'https://example.com/x.png#one'},{url:'https://example.com/x.png#two'}]);
    expect(gallery.images()[0].error).toContain('HTTPS');expect(gallery.images()[2].error).toContain('duplicada');
    expect(gallery.hasPendingOrFailedUploads()).toBe(true);
  });
  it('keeps an oversized restored gallery visible and invalid until excess images are removed',()=>{
    gallery.setImages(Array.from({length:9},(_,i)=>({url:`https://example.com/${i}.png`})));
    expect(gallery.images()).toHaveLength(9);expect(gallery.hasPendingOrFailedUploads()).toBe(true);
    gallery.remove(gallery.images()[8]);expect(gallery.hasPendingOrFailedUploads()).toBe(false);
  });
  it('does not append or upload a decoded file after the editor is destroyed',async()=>{
    let resolve!:()=>void;vi.stubGlobal('Image',class{src='';decode(){return new Promise<void>(r=>resolve=r);}});
    const adding=gallery.addFiles([file()]);expect(gallery.hasPendingUploads()).toBe(true);gallery.ngOnDestroy();resolve();await adding;
    expect(gallery.images()).toEqual([]);expect(storage.upload).not.toHaveBeenCalled();expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:decoded');
  });
  it('decodes design files locally without contacting Storage',async()=>{
    const fixture=TestBed.createComponent(ProductMedia);fixture.componentRef.setInput('isDesignMode',true);fixture.detectChanges();
    vi.stubGlobal('Image',class{src='';async decode(){}});await fixture.componentInstance.addFiles([file()]);
    expect(storage.upload).not.toHaveBeenCalled();expect(fixture.componentInstance.images()).toHaveLength(1);
    expect(fixture.componentInstance.snapshot()[0].url).toBeNull();fixture.destroy();
  });
});

class FakeXhr {
  static DONE=4; static requests:FakeXhr[]=[];
  readyState=1; status=200;responseText='';timeout=0;
  headers:Record<string,string>={};events=new Map<string,()=>void>();upload={addEventListener:vi.fn()};
  open=vi.fn();send=vi.fn();abort=vi.fn(()=>{this.readyState=4;this.events.get('abort')?.();});
  constructor(){FakeXhr.requests.push(this);}
  addEventListener(name:string,fn:()=>void){this.events.set(name,fn);}
  setRequestHeader(name:string,value:string){this.headers[name]=value;}
  finish(status=200){this.status=status;this.readyState=4;this.events.get('load')?.();}
}
describe('Phase three authenticated Storage transport',()=>{
  let session:ReturnType<typeof signal<any>>;
  beforeEach(()=>{
    FakeXhr.requests=[];vi.stubGlobal('XMLHttpRequest',FakeXhr);session=signal({access_token:'test-session'});
    TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:{session}}]});
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.unstubAllGlobals();vi.restoreAllMocks();});
  it('requires authentication for uploads and deletion instead of falling back to the public key',()=>{
    session.set(null);const service=TestBed.inject(SupabaseStorageService);let error='';
    service.upload(new File(['x'],'x.png',{type:'image/png'})).subscribe(status=>error=status.error??'');
    service.delete('productos/x.png').subscribe();expect(error).toContain('sesión');expect(FakeXhr.requests).toHaveLength(0);
  });
  it('uses unique non-overwriting uploads, a timeout, and aborts on unsubscribe',()=>{
    const service=TestBed.inject(SupabaseStorageService);const file=new File(['x'],'x.png',{type:'image/png'});
    const sub=service.upload(file).subscribe();const xhr=FakeXhr.requests[0];expect(xhr.headers['x-upsert']).toBe('false');
    expect(xhr.timeout).toBe(45000);expect(service.generatePath('x.png')).not.toBe(service.generatePath('x.png'));
    sub.unsubscribe();expect(xhr.abort).toHaveBeenCalled();
  });
  it('rejects traversal paths without sending a request',()=>{
    const service=TestBed.inject(SupabaseStorageService);service.delete('productos/../saved.png').subscribe();expect(FakeXhr.requests).toHaveLength(0);
  });
  it('retains failed cleanup across editors and removes it only after a successful retry',()=>{
    const service=TestBed.inject(SupabaseStorageService);service.queueCleanup(['productos/temp.png']);
    FakeXhr.requests[0].finish(403);expect(service.cleanupFailures()).toEqual(['productos/temp.png']);
    service.retryCleanup();FakeXhr.requests[1].finish(200);expect(service.cleanupFailures()).toEqual([]);
  });
  it('reports upload timeouts as errors and treats missing cleanup files as already removed',()=>{
    const service=TestBed.inject(SupabaseStorageService);let error='';
    service.upload(new File(['x'],'x.png',{type:'image/png'})).subscribe(status=>error=status.error??'');
    FakeXhr.requests[0].events.get('timeout')?.();expect(error).toContain('demasiado');
    service.queueCleanup(['productos/missing.png']);FakeXhr.requests[1].finish(404);expect(service.cleanupFailures()).toEqual([]);
  });
});
