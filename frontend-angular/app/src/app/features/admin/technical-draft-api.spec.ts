import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TechnicalDraftApi, TechnicalDraftDetail } from './technical-draft-api';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

describe('Technical draft API contracts',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]}));
  afterEach(()=>TestBed.inject(HttpTestingController).verify());
  it('lists with server pagination and optional search/state',()=>{
    TestBed.inject(TechnicalDraftApi).list(2,'  CPU  ','DRAFT').subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne(r=>r.url==='/api/v1/admin/borradores-tecnicos');
    expect(request.request.params.get('page')).toBe('2');expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('q')).toBe('CPU');expect(request.request.params.get('state')).toBe('DRAFT');
    request.flush({content:[],page:2,totalPages:3,totalElements:21});
  });
  it('creates a document without client-supplied ownership or verification authority',()=>{
    TestBed.inject(TechnicalDraftApi).create({schema:'rya.technical-draft.v1'}).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/borradores-tecnicos');
    expect(request.request.method).toBe('POST');expect(request.request.body).toEqual({document:{schema:'rya.technical-draft.v1'}});
    request.flush({});
  });
  it('updates the exact ID and last server version rather than creating a replacement',()=>{
    TestBed.inject(TechnicalDraftApi).update({id:'draft-id',version:7} as TechnicalDraftDetail,{schema:'rya.technical-draft.v1'}).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/borradores-tecnicos/draft-id');
    expect(request.request.method).toBe('PUT');expect(request.request.body.version).toBe(7);request.flush({});
  });
});
