import { Injectable, inject, signal } from '@angular/core';
import { Observable, Observer } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

export interface StorageUploadProgress {
  progress: number;
  url?: string;
  error?: string;
  path?: string;
}

@Injectable({ providedIn: 'root' })
export class SupabaseStorageService {
  private readonly auth = inject(AuthService, { optional: true });

  readonly bucketName = 'productos';
  readonly supabaseUrl = environment.supabaseUrl;
  readonly anonKey = environment.supabaseAnonKey;

  readonly cleanupFailures = signal<readonly string[]>([]);
  private readonly cleaning = new Set<string>();

  /** Only temporary paths explicitly supplied by the editor, never inferred from existing image URLs. */
  queueCleanup(paths: Iterable<string>): void {
    for (const path of paths) {
      if (this.cleaning.has(path)) continue;
      this.cleaning.add(path);
      this.delete(path).subscribe({
        next: result => {
          this.cleaning.delete(path);
          this.cleanupFailures.update(list => result.success ? list.filter(p => p !== path) : [...new Set([...list,path])]);
        },
        error: () => { this.cleaning.delete(path); this.cleanupFailures.update(list => [...new Set([...list,path])]); }
      });
    }
  }

  retryCleanup(): void { this.queueCleanup(this.cleanupFailures()); }

  private validPath(path: string): boolean { return /^productos\/[a-zA-Z0-9._-]+$/.test(path) && !path.includes('..'); }

  /**
   * Genera una ruta sanitizada y única para guardar la imagen en Supabase Storage
   */
  generatePath(fileName: string): string {
    const cleanName = (fileName || 'imagen')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9.-]/g, '_');
    return `productos/${crypto.randomUUID()}-${cleanName.slice(-100)}`;
  }

  /**
   * Obtiene la URL pública CDN en Supabase Storage
   */
  getPublicUrl(path: string): string {
    return `${this.supabaseUrl}/storage/v1/object/public/${this.bucketName}/${path}`;
  }

  /**
   * Sube una imagen a Supabase Storage con seguimiento de progreso (0% a 100%)
   * y control detallado de errores y reintentos.
   */
  upload(file: File, customPath?: string): Observable<StorageUploadProgress> {
    const path = customPath || this.generatePath(file.name);
    return new Observable<StorageUploadProgress>((observer: Observer<StorageUploadProgress>) => {
      const token = this.auth?.session()?.access_token;
      if (!token || !this.validPath(path) || !['image/jpeg','image/png','image/webp','image/avif'].includes(file.type) || file.size === 0 || file.size > 8 * 1024 * 1024) {
        observer.next({progress:0,error:!token ? 'Inicia sesión para subir imágenes.' : 'Archivo o ruta no válidos.'}); observer.complete(); return;
      }
      observer.next({ progress: 0, path });

      const xhr = new XMLHttpRequest();
      const uploadUrl = `${this.supabaseUrl}/storage/v1/object/${this.bucketName}/${path}`;

      xhr.upload.addEventListener('progress', (event: ProgressEvent) => {
        if (event.lengthComputable) {
          const percent = Math.round((event.loaded / event.total) * 95);
          observer.next({ progress: Math.min(percent, 95), path });
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          const publicUrl = this.getPublicUrl(path);
          observer.next({ progress: 100, url: publicUrl, path });
          observer.complete();
        } else {
          let errorMsg = `Error al subir imagen a Supabase (HTTP ${xhr.status})`;
          try {
            const body = JSON.parse(xhr.responseText);
            if (body.message) errorMsg = body.message;
            else if (body.error) errorMsg = body.error;
          } catch {
            if (xhr.status === 404) {
              errorMsg = `El bucket "${this.bucketName}" no existe en Supabase Storage.`;
            } else if (xhr.status === 403 || xhr.status === 401) {
              errorMsg = 'Permiso denegado en Supabase Storage. Revisa las políticas RLS.';
            }
          }
          observer.next({ progress: 0, error: errorMsg, path });
          observer.complete();
        }
      });

      xhr.addEventListener('error', () => {
        observer.next({
          progress: 0,
          error: 'Fallo de conexión al subir la imagen a Supabase Storage.',
          path
        });
        observer.complete();
      });

      xhr.addEventListener('abort', () => {
        observer.next({ progress: 0, error: 'Subida cancelada por el usuario.', path });
        observer.complete();
      });
      xhr.addEventListener('timeout', () => {
        observer.next({progress:0,error:'La subida tardó demasiado. Reintenta la operación.',path});observer.complete();
      });

      xhr.open('POST', uploadUrl, true);
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.setRequestHeader('apiKey', this.anonKey);
      xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');
      xhr.timeout = 45000;
      xhr.setRequestHeader('x-upsert', 'false');
      xhr.send(file);

      return () => {
        if (xhr.readyState !== XMLHttpRequest.DONE) {
          xhr.abort();
        }
      };
    });
  }

  /**
   * Elimina un archivo de Supabase Storage
   */
  delete(path: string): Observable<{ success: boolean; error?: string }> {
    const deleteUrl = `${this.supabaseUrl}/storage/v1/object/${this.bucketName}/${path}`;

    return new Observable(observer => {
      const token = this.auth?.session()?.access_token;
      if (!token || !this.validPath(path)) {
        observer.next({success:false,error:!token ? 'Inicia sesión para limpiar archivos temporales.' : 'Ruta temporal no válida.'});observer.complete();return;
      }
      const xhr = new XMLHttpRequest();
      xhr.addEventListener('load', () => {
        if ((xhr.status >= 200 && xhr.status < 300) || xhr.status === 404) {
          observer.next({ success: true });
          observer.complete();
        } else {
          observer.next({ success: false, error: 'No se pudo eliminar el archivo en Storage.' });
          observer.complete();
        }
      });
      xhr.addEventListener('error', () => {
        observer.next({ success: false, error: 'Error de conexión.' });
        observer.complete();
      });
      xhr.addEventListener('timeout', () => { observer.next({success:false,error:'La limpieza tardó demasiado.'});observer.complete(); });
      xhr.addEventListener('abort', () => { observer.next({success:false,error:'Limpieza cancelada.'});observer.complete(); });
      xhr.open('DELETE', deleteUrl, true);
      xhr.timeout = 30000;
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.setRequestHeader('apiKey', this.anonKey);
      xhr.send();
      return () => { if (xhr.readyState !== XMLHttpRequest.DONE) xhr.abort(); };
    });
  }
}
