import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface TechnicalDraftDetail {
  id: string; version: number; state: string; verificationStatus: string;
  createdBy: string; updatedBy: string; createdAt: string; updatedAt: string;
  document: Record<string, unknown>;
}
export interface TechnicalDraftSummary {
  id: string; version: number; name: string; sku: string; type: string; state: string;
  createdBy: string; updatedAt: string;
}
export interface TechnicalDraftPage {
  content: TechnicalDraftSummary[]; page: number; totalPages: number; totalElements: number;
}
export interface DraftEvent {id:string;version:number;actor:string;action:string;state:string;comment:string;createdAt:string;}
export interface DraftFicha {
  productId:string;sku:string;name:string;linked:boolean;sourceId:string|null;sourceUrl:string|null;verifiedAt:string|null;
  fields:Array<{key:string;unit:string|null;current:unknown;proposed:unknown;status:string}>;warnings:string[];
}

/** JWT authorization is supplied by the existing HTTP interceptor, never by form values. */
@Injectable({providedIn:'root'})
export class TechnicalDraftApi {
  private readonly http=inject(HttpClient);
  private readonly base='/api/v1/admin/borradores-tecnicos';
  list(page=0, query='', state='') {
    let params=new HttpParams().set('page',page).set('size',10);
    if(query.trim())params=params.set('q',query.trim());
    if(state)params=params.set('state',state);
    return this.http.get<TechnicalDraftPage>(this.base,{params});
  }
  get(id:string){return this.http.get<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(id)}`);}
  create(document:Record<string,unknown>){return this.http.post<TechnicalDraftDetail>(this.base,{document});}
  update(draft:TechnicalDraftDetail,document:Record<string,unknown>){
    return this.http.put<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(draft.id)}`,{version:draft.version,document});
  }
  ficha(id:string){return this.http.get<DraftFicha>(`${this.base}/${encodeURIComponent(id)}/ficha`);}
  link(draft:TechnicalDraftDetail,unlink=false){return this.http.post<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(draft.id)}/vinculo`,{version:draft.version},{params:{unlink}});}
  submit(draft:TechnicalDraftDetail){return this.http.post<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(draft.id)}/revision`,{version:draft.version});}
  decide(draft:TechnicalDraftDetail,decision:'APPROVED'|'REJECTED',comment:string){return this.http.post<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(draft.id)}/decision`,{version:draft.version,decision,comment});}
  reopen(draft:TechnicalDraftDetail,comment:string){return this.http.post<TechnicalDraftDetail>(`${this.base}/${encodeURIComponent(draft.id)}/reabrir`,{version:draft.version,comment});}
  history(id:string,page=0){return this.http.get<{content:DraftEvent[];page:number;totalPages:number}>(`${this.base}/${encodeURIComponent(id)}/historial`,{params:{page,size:10}});}
  revision(id:string,version:number){return this.http.get<{event:DraftEvent;document:Record<string,unknown>}>(`${this.base}/${encodeURIComponent(id)}/versiones/${version}`);}
}
