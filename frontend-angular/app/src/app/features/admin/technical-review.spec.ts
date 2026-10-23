import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TechnicalReview } from './technical-review';
import { TechnicalDraftApi, TechnicalDraftDetail } from './technical-draft-api';
import { AuthService } from '../../core/auth/auth.service';

describe('Administrative technical review',()=>{
  let api:any;let user='author';
  const draft=(overrides:Partial<TechnicalDraftDetail>={}):TechnicalDraftDetail=>({id:'draft',version:2,state:'DRAFT',createdBy:'author',updatedBy:'author',createdAt:'',updatedAt:'',verificationStatus:'pending',document:{},...overrides});
  beforeEach(()=>{user='author';api={ficha:vi.fn(()=>of({productId:'product',fields:[],warnings:[],linked:false})),link:vi.fn(()=>of(draft({version:3}))),submit:vi.fn(()=>of(draft({state:'IN_REVIEW',version:3}))),decide:vi.fn(()=>of(draft({state:'APPROVED',version:3}))),reopen:vi.fn(()=>of(draft())),history:vi.fn(()=>of({content:[],page:0,totalPages:0})),revision:vi.fn(()=>of({document:{},event:{}}))};
    TestBed.configureTestingModule({imports:[TechnicalReview],providers:[{provide:TechnicalDraftApi,useValue:api},{provide:AuthService,useValue:{user:()=>({id:user})}}]});
    vi.spyOn(window,'confirm').mockReturnValue(true);
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.restoreAllMocks();});
  function open(value=draft()){const fixture=TestBed.createComponent(TechnicalReview);fixture.componentRef.setInput('draft',value);fixture.detectChanges();return {fixture,review:fixture.componentInstance};}
  it('does not fetch implicitly or send a dirty version',()=>{const {fixture,review}=open();expect(api.ficha).not.toHaveBeenCalled();fixture.componentRef.setInput('disabled',true);fixture.detectChanges();review.compare();review.submit();expect(api.ficha).not.toHaveBeenCalled();expect(api.submit).not.toHaveBeenCalled();});
  it('requires a real comparison before linking and uses confirmed version',()=>{const {review}=open();review.link();expect(api.link).not.toHaveBeenCalled();review.compare();review.link();expect(api.link).toHaveBeenCalledWith(expect.objectContaining({version:2}),false);});
  it('blocks self-approval and requires a review reason',()=>{const {review}=open(draft({state:'IN_REVIEW'}));review.comment='Checked';review.decide('APPROVED');expect(api.decide).not.toHaveBeenCalled();user='reviewer';review.comment=' ';review.decide('APPROVED');expect(api.decide).not.toHaveBeenCalled();review.comment='Sources checked';review.decide('APPROVED');expect(api.decide).toHaveBeenCalledWith(expect.objectContaining({version:2}),'APPROVED','Sources checked');});
  it('blocks double submission while waiting for the server',()=>{const {review}=open();api.submit.mockReturnValue(new Subject());review.submit();review.submit();expect(api.submit).toHaveBeenCalledTimes(1);expect(review.busy()).toBe(true);});
  it('reports uncertain writes and conflicts instead of announcing success',()=>{const {review}=open();const emit=vi.spyOn(review.unresolved,'emit');api.submit.mockReturnValue(throwError(()=>({status:0})));review.submit();expect(emit).toHaveBeenCalledWith(0);expect(review.notice()).toBe('');api.submit.mockReturnValue(throwError(()=>({status:409,error:{message:'Recarga'}})));review.submit();expect(emit).toHaveBeenCalledWith(409);});
  it('cannot reopen or submit another author’s document',()=>{user='reviewer';const {fixture,review}=open();review.submit();fixture.componentRef.setInput('draft',draft({state:'REJECTED'}));fixture.detectChanges();review.reopen();expect(api.submit).not.toHaveBeenCalled();expect(api.reopen).not.toHaveBeenCalled();});
  it('clears old comparison and cancels stale reads when another draft opens',()=>{const {fixture,review}=open();const pending=new Subject<any>();api.ficha.mockReturnValue(pending);review.compare();fixture.componentRef.setInput('draft',draft({id:'another'}));fixture.detectChanges();pending.next({productId:'stale'});expect(review.ficha()).toBeNull();expect(review.reading()).toBe(false);});
});
