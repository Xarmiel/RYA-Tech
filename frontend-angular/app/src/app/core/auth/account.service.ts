import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { NotificationService } from '../notifications/notification.service';

export interface UserProfile {
  id: string;
  nombre: string;
  email: string;
  telefono: string | null;
  estado: string;
  emailVerificado: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly notifications = inject(NotificationService);
  readonly profile = signal<UserProfile | null>(null);

  load(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/v1/cuenta/perfil').pipe(tap(profile => this.profile.set(profile)));
  }

  update(nombre: string, telefono: string): Observable<UserProfile> {
    return this.http.put<UserProfile>('/api/v1/cuenta/perfil', { nombre, telefono })
      .pipe(tap(profile => { this.profile.set(profile); this.notifications.show('Perfil actualizado.', 'success'); }));
  }

  clear(): void { this.profile.set(null); }
}
