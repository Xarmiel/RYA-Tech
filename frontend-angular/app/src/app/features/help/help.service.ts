import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface HelpQuestion { id: string; pregunta: string; respuesta: string; categoriaId: string | null; categoria: string; }
export interface HelpPage { content: HelpQuestion[]; categorias: Array<{ id: string; nombre: string }>; page: number; size: number; totalElements: number; totalPages: number; }
export interface SupportContact { email: string | null; telefono: string | null; telefonoUrl: string | null; whatsappUrl: string | null; horario: string | null; }

@Injectable({ providedIn: 'root' })
export class HelpService {
  private readonly http = inject(HttpClient);
  questions(q: string, category: string, page: number): Observable<HelpPage> {
    let params = new HttpParams().set('page',page).set('size',12);
    if (q.trim()) params = params.set('q',q.trim());
    if (category) params = params.set('categoriaId',category);
    return this.http.get<HelpPage>('/api/v1/ayuda/preguntas',{params});
  }
  contact(): Observable<SupportContact> { return this.http.get<SupportContact>('/api/v1/ayuda/contacto'); }
}
