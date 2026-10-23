import { Component, OnDestroy, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';

export interface DraftImage {
  id: number;
  url: string;
  alt: string;
  file?: File;
  uploading?: boolean;
  progress?: number;
  error?: string;
  previewError?: string;
  checkingPreview?: boolean;
  supabasePath?: string;
}

export function imageUrl(raw: string): string {
  const url = new URL(raw.trim());
  if (url.protocol !== 'https:' || url.username || url.password) {
    throw new Error('Usa una URL HTTPS sin credenciales.');
  }
  url.hash = '';
  return url.href;
}

@Component({
  selector: 'app-product-media',
  imports: [FormsModule],
  styleUrl: './product-media.scss',
  template: `
    <section class="editor-section product-media-section">
      <div class="media-header">
        <div>
          <h3>Galería de imágenes</h3>
          <p class="media-help">{{ isDesignMode() ? 'Vista local: los archivos solo se previsualizan; no se suben ni se guardan como enlaces.' : 'Sube archivos a Supabase Storage o añade enlaces HTTPS.' }} La primera imagen es la principal (hasta 8 imágenes combinadas).</p>
        </div>
        <span class="media-counter" [class.counter-max]="images().length >= 8">
          {{ images().length }} / 8 imágenes
        </span>
      </div>
      <p class="media-help">Para reemplazar una imagen, retírala y añade la nueva. Retirar una imagen existente solo cambia la galería al guardar; no borra su archivo de Storage.</p>

      <div class="dropzone" (dragover)="$event.preventDefault()" (drop)="drop($event)">
        <span class="material-symbols-rounded dropzone-icon">cloud_upload</span>
        <label class="dropzone-label">
          <span>Arrastra imágenes aquí o haz clic para seleccionarlas</span>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/avif"
            multiple
            [disabled]="images().length + decoding() >= 8"
            (change)="select($event)"
          >
        </label>
        <small class="dropzone-hint">Formatos: JPG, PNG, WebP o AVIF · Máx. 8 MB por imagen</small>
      </div>
      @if (decoding()) { <p role="status">Validando {{ decoding() }} imagen(es)…</p> }
      @if (cleanupFailures().length) {
        <div class="admin-alert" role="alert">Quedan {{ cleanupFailures().length }} archivo(s) temporal(es) por limpiar. <button type="button" (click)="retryCleanup()">Reintentar limpieza</button></div>
      }
      @if (uncertainSave()) { <p role="alert">No se confirmó el resultado del guardado. Los archivos se conservarán para evitar borrar imágenes que el servidor pudo haber asociado. Revisa el producto antes de retirarlos.</p> }

      <div class="inline-controls url-input-row">
        <label class="url-input-label">
          <span>Enlace HTTPS externo</span>
          <input
            [(ngModel)]="url"
            [ngModelOptions]="{ standalone: true }"
            (keydown.enter)="$event.preventDefault(); addUrl()"
            placeholder="https://ejemplo.com/producto.webp"
            [disabled]="images().length >= 8"
          >
        </label>
        <button
          type="button"
          class="button button-secondary btn-add-url"
          (click)="addUrl()"
          [disabled]="images().length >= 8 || !url.trim()"
        >
          <span class="material-symbols-rounded">add_link</span>
          <span>Añadir enlace</span>
        </button>
      </div>

      @if (error()) {
        <div class="admin-alert admin-alert-error" role="alert">
          <span class="material-symbols-rounded">error</span>
          <span>{{ error() }}</span>
        </div>
      }

      @if (images().length > 0) {
        <div class="media-grid">
          @for (item of images(); track item.id; let i = $index) {
            <article class="media-card" [class.media-card-primary]="i === 0">
              <div class="media-card-preview">
                @if (item.previewError || (!item.url.startsWith('https://') && !item.url.startsWith('blob:'))) {
                  <span class="preview-unavailable"><span class="material-symbols-rounded" aria-hidden="true">broken_image</span>Vista previa no disponible</span>
                } @else {
                <img
                  [src]="item.url"
                  [alt]="item.alt || 'Vista previa ' + (i + 1)"
                  referrerpolicy="no-referrer"
                  (error)="onImageLoadError(item)"
                  (load)="onImageLoaded(item)"
                >
                }
                <div class="media-badge" [class.badge-primary]="i === 0">
                  {{ i === 0 ? '★ Principal' : '#' + (i + 1) }}
                </div>
              </div>

              @if (item.uploading) {
                <div class="upload-status-box">
                  <div class="upload-progress-container" role="progressbar" aria-label="Subida de imagen" aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="item.progress ?? 0">
                    <div class="upload-progress-fill" [style.width.%]="item.progress ?? 0"></div>
                  </div>
                  <div class="upload-status-text">
                    <span class="material-symbols-rounded spin-icon">sync</span>
                    <span>Subiendo a Supabase ({{ item.progress ?? 0 }}%)...</span>
                  </div>
                </div>
              }
              @if (item.previewError) {
                <div class="upload-error-box" role="alert"><div class="upload-error-content"><small>{{ item.previewError }}</small><button class="btn-retry" type="button" (click)="retryPreview(item)" [disabled]="item.checkingPreview">{{ item.checkingPreview ? 'Comprobando…' : 'Reintentar vista previa' }}</button></div></div>
              }

              @if (item.error) {
                <div class="upload-error-box" role="alert">
                  <span class="material-symbols-rounded error-icon">warning</span>
                  <div class="upload-error-content">
                    <small>{{ item.error }}</small>
                    @if (item.file) {
                      <button type="button" class="btn-retry" (click)="retryUpload(item)">
                        Reintentar subida
                      </button>
                    }
                  </div>
                </div>
              }

              <label class="media-alt-label">
                <span>Texto alternativo</span>
                <input
                  [(ngModel)]="item.alt"
                  [ngModelOptions]="{ standalone: true }"
                  (ngModelChange)="changed.emit()"
                  placeholder="Descripción de la imagen"
                  maxlength="300"
                  [attr.aria-label]="'Texto alternativo de imagen ' + (i + 1)"
                >
              </label>

              <div class="media-actions">
                @if (i > 0) {
                  <button
                    type="button"
                    class="btn-make-primary"
                    (click)="makePrimary(i)"
                    title="Definir como imagen principal"
                  >
                    Hacer principal
                  </button>
                }
                <div class="order-buttons">
                  <button
                    type="button"
                    [disabled]="i === 0"
                    (click)="move(i, -1)"
                    title="Mover antes"
                    aria-label="Mover imagen antes"
                  >
                    ←
                  </button>
                  <button
                    type="button"
                    [disabled]="i === images().length - 1"
                    (click)="move(i, 1)"
                    title="Mover después"
                    aria-label="Mover imagen después"
                  >
                    →
                  </button>
                </div>
                <button
                  type="button"
                  class="btn-delete"
                  (click)="remove(item)"
                  title="Eliminar de la galería"
                >
                  Eliminar
                </button>
              </div>
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class ProductMedia implements OnDestroy {
  private readonly storage = inject(SupabaseStorageService, { optional: true });
  private readonly uploadSubs = new Map<number, Subscription>();
  private readonly stagedUploadPaths = new Set<string>();
  private readonly protectedPaths = new Set<string>();
  private readonly decodingUrls = new Set<string>();
  private destroyed = false;
  private generation = 0;

  readonly isDesignMode = input<boolean>(false);
  readonly changed = output<void>();
  readonly images = signal<DraftImage[]>([]);
  readonly error = signal('');
  readonly decoding = signal(0);
  readonly uncertainSave = signal(false);
  readonly cleanupFailures = computed(() => this.storage?.cleanupFailures?.() ?? []);

  url = '';
  private sequence = 0;

  addUrl(): void {
    try {
      const sanitized = imageUrl(this.url);
      if (this.images().length >= 8) throw new Error('El límite es de ocho imágenes.');
      if (this.images().some(i => i.url === sanitized)) throw new Error('Ese enlace ya está añadido.');
      this.images.update(items => [
        ...items,
        { id: ++this.sequence, url: sanitized, alt: '', uploading: false, progress: 100 }
      ]);
      this.url = '';
      this.error.set('');
      this.changed.emit();
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Enlace inválido.');
    }
  }

  select(event: Event): void {
    const input = event.target as HTMLInputElement;
    void this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  drop(event: DragEvent): void {
    event.preventDefault();
    void this.addFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  async addFiles(files: File[]): Promise<void> {
    const generation = this.generation;
    for (const file of files) {
      if (this.destroyed || generation !== this.generation) return;
      if (this.images().length + this.decoding() >= 8) {
        this.error.set('El límite es de ocho imágenes.');
        break;
      }
      if (!['image/jpeg', 'image/png', 'image/webp', 'image/avif'].includes(file.type) || file.size === 0 || file.size > 8 * 1024 * 1024) {
        this.error.set('Archivo no válido o mayor de 8 MB.');
        continue;
      }
      if (this.images().some(i => i.file?.name === file.name && i.file?.size === file.size && i.file?.lastModified === file.lastModified)) {
        this.error.set('Ese archivo ya está añadido.');
        continue;
      }

      const previewUrl = URL.createObjectURL(file);
      this.decodingUrls.add(previewUrl);
      this.decoding.update(value => value + 1);
      try {
        const image = new Image();
        image.src = previewUrl;
        await image.decode();

        if (this.destroyed || generation !== this.generation || this.images().length >= 8 || this.images().some(i => i.file?.name === file.name && i.file?.size === file.size && i.file?.lastModified === file.lastModified)) {
          URL.revokeObjectURL(previewUrl);
          break;
        }

        const isDesign = this.isDesignMode();
        const draft: DraftImage = {
          id: ++this.sequence,
          url: previewUrl,
          alt: file.name.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' '),
          file,
          uploading: !isDesign && !!this.storage,
          progress: isDesign ? 100 : 0,
          error: !isDesign && !this.storage ? 'No está disponible el servicio de imágenes.' : undefined,
        };

        this.images.update(items => [...items, draft]);
        this.changed.emit();

        if (!isDesign && this.storage) {
          this.startUpload(draft);
        }
      } catch {
        URL.revokeObjectURL(previewUrl);
        if (!this.destroyed && generation === this.generation) this.error.set('El archivo no contiene una imagen válida.');
      } finally {
        this.decodingUrls.delete(previewUrl);
        this.decoding.update(value => Math.max(0, value - 1));
      }
    }
  }

  startUpload(item: DraftImage): void {
    if (this.destroyed || !this.images().includes(item) || this.uploadSubs.get(item.id)?.closed === false) return;
    if (!item.file || !this.storage || this.isDesignMode()) {
      item.uploading = false;
      return;
    }

    item.uploading = true;
    item.error = undefined;
    item.progress = 0;
    this.images.set([...this.images()]);

    const generation = this.generation;
    const sub = this.storage.upload(item.file).subscribe({
      next: status => {
        if (this.destroyed || generation !== this.generation || !this.images().includes(item)) return;
        if (status.path) {
          item.supabasePath = status.path;
          this.stagedUploadPaths.add(status.path);
        }
        if (status.progress !== undefined) item.progress = status.progress;
        if (status.url) {
          if (item.url.startsWith('blob:')) URL.revokeObjectURL(item.url);
          item.url = status.url;
          item.uploading = false;
          item.error = undefined;
          item.previewError = undefined;
        }
        if (status.error) {
          item.uploading = false;
          item.error = status.error;
        }
        this.images.set([...this.images()]);
        this.changed.emit();
      },
      error: err => {
        if (this.destroyed || generation !== this.generation || !this.images().includes(item)) return;
        item.uploading = false;
        item.error = err?.message || 'Error de conexión al subir la imagen.';
        this.images.set([...this.images()]);
        this.changed.emit();
      },
      complete: () => {
        if (!this.destroyed && generation === this.generation && this.images().includes(item) && item.uploading) {
          item.uploading = false; item.error = 'No se recibió la confirmación de la subida. Reintenta antes de guardar.';
          this.images.set([...this.images()]); this.changed.emit();
        }
      }
    });

    this.uploadSubs.set(item.id, sub);
  }

  retryUpload(item: DraftImage): void {
    if (item.file && !item.uploading) {
      if (item.supabasePath && this.stagedUploadPaths.has(item.supabasePath) && !this.protectedPaths.has(item.supabasePath)) this.cleanupPaths([item.supabasePath]);
      item.supabasePath = undefined;
      this.startUpload(item);
    }
  }

  makePrimary(index: number): void {
    if (index <= 0 || index >= this.images().length) return;
    this.images.update(items => {
      const next = [...items];
      const [selected] = next.splice(index, 1);
      next.unshift(selected);
      return next;
    });
    this.changed.emit();
  }

  move(index: number, by: number): void {
    if (!Number.isInteger(index) || ![-1,1].includes(by) || index < 0 || index >= this.images().length || index + by < 0 || index + by >= this.images().length) return;
    this.images.update(items => {
      const next = [...items];
      [next[index], next[index + by]] = [next[index + by], next[index]];
      return next;
    });
    this.changed.emit();
  }

  remove(item: DraftImage): void {
    this.uploadSubs.get(item.id)?.unsubscribe();
    this.uploadSubs.delete(item.id);
    if (item.file && item.url.startsWith('blob:')) {
      URL.revokeObjectURL(item.url);
    }
    if (item.supabasePath && this.storage && !this.isDesignMode()) {
      if (this.stagedUploadPaths.has(item.supabasePath)) {
        if (!this.protectedPaths.has(item.supabasePath)) this.cleanupPaths([item.supabasePath]);
      }
    }
    this.images.update(items => items.filter(i => i.id !== item.id));
    this.changed.emit();
  }

  onImageLoadError(item: DraftImage): void {
    if (this.destroyed || !this.images().includes(item)) return;
    item.previewError = 'No se pudo cargar esta imagen. Reintenta la vista previa o retírala antes de guardar.';
    this.images.set([...this.images()]); this.changed.emit();
  }

  onImageLoaded(item: DraftImage): void {
    if (this.destroyed || !item.previewError || !this.images().includes(item)) return;
    item.previewError = undefined; this.images.set([...this.images()]); this.changed.emit();
  }

  async retryPreview(item: DraftImage): Promise<void> {
    if (item.checkingPreview || this.destroyed || !this.images().includes(item)) return;
    item.checkingPreview = true; this.images.set([...this.images()]);
    try { const image = new Image(); image.src = item.url; await image.decode(); this.onImageLoaded(item); }
    catch { this.onImageLoadError(item); }
    finally { item.checkingPreview = false; if (!this.destroyed && this.images().includes(item)) this.images.set([...this.images()]); }
  }

  /**
   * Carga una galería existente (ej. al abrir la edición de un producto)
   */
  setImages(list: Array<{ url: string; alt?: string; textoAlternativo?: string; primary?: boolean; principal?: boolean; order?: number; posicion?: number }>): void {
    this.generation++;
    for (const sub of this.uploadSubs.values()) sub.unsubscribe();
    this.uploadSubs.clear();
    this.cleanupStagedUploads();
    for (const item of this.images()) if (item.url.startsWith('blob:')) URL.revokeObjectURL(item.url);
    const sorted = [...(list || [])].sort((a, b) => {
      const aPrim = a.primary || a.principal;
      const bPrim = b.primary || b.principal;
      if (aPrim && !bPrim) return -1;
      if (!aPrim && bPrim) return 1;
      const posA = a.posicion ?? a.order ?? 0;
      const posB = b.posicion ?? b.order ?? 0;
      return posA - posB;
    });

    const seen = new Set<string>();
    const items: DraftImage[] = sorted.map(img => {
      let url = img.url ?? ''; let error: string | undefined;
      try { url = imageUrl(url); if (seen.has(url)) error = 'Imagen duplicada. Retírala antes de guardar.'; seen.add(url); }
      catch { error = 'El enlace guardado no es una URL HTTPS válida. Retira o reemplaza esta imagen.'; }
      return {
        id: ++this.sequence,
        url,
        alt: img.alt || img.textoAlternativo || '',
        error,
        uploading: false,
        progress: 100
      };
    });

    this.images.set(items);
    this.error.set('');
    this.changed.emit();
  }

  /**
   * Indica si hay imágenes en proceso de subida
   */
  hasPendingUploads(): boolean {
    return this.decoding() > 0 || this.images().some(i => i.uploading || i.checkingPreview);
  }

  /**
   * Indica si hay imágenes pendientes, con error o que continúen como blob: temporal
   */
  hasPendingOrFailedUploads(): boolean {
    return this.hasPendingUploads() || this.images().length > 8 || this.images().some(i => !!i.error || !!i.previewError || !i.url.startsWith('https://'));
  }

  /**
   * Limpia archivos subidos durante la sesión del modal que no llegaron a confirmarse
   */
  cleanupStagedUploads(): void {
    if (!this.storage || this.isDesignMode()) {
      this.stagedUploadPaths.clear();
      return;
    }
    this.cleanupPaths([...this.stagedUploadPaths].filter(path => !this.protectedPaths.has(path)));
  }

  private cleanupPaths(paths: string[]): void {
    if (!paths.length || !this.storage || this.isDesignMode()) return;
    if (this.storage.queueCleanup) {
      this.storage.queueCleanup(paths);
      paths.forEach(path => this.stagedUploadPaths.delete(path));
    } else {
      // Compatible with isolated test doubles; production uses the service's retry queue.
      for (const path of paths) this.storage.delete(path).subscribe({
        next: result => { if (result?.success) this.stagedUploadPaths.delete(path); },
        error: () => this.error.set('Quedan archivos temporales por limpiar.'),
      });
    }
  }

  retryCleanup(): void { this.storage?.retryCleanup?.(); }

  beginPersistenceAttempt(): void { for (const path of this.stagedUploadPaths) this.protectedPaths.add(path); }
  persistenceFailed(status?: number): void {
    if (status && status >= 400 && status < 500 && status !== 408 && !this.uncertainSave()) this.protectedPaths.clear();
    else this.uncertainSave.set(true);
  }

  /**
   * Confirma las subidas staged para que no sean eliminadas al cerrar el modal
   */
  commitStagedUploads(): void {
    this.stagedUploadPaths.clear();
    this.protectedPaths.clear(); this.uncertainSave.set(false);
  }

  /**
   * Retorna el snapshot compatible con TechnicalEditor y pruebas
   */
  snapshot(): Array<{ order: number; primary: boolean; alt: string; url: string | null; file: { name: string; type: string; size: number; uploadPending: boolean } | null }> {
    return this.images().map((i, index) => ({
      order: index + 1,
      primary: index === 0,
      alt: i.alt,
      url: i.file && !i.url.startsWith('https:') ? null : i.url,
      file: i.file ? {
        name: i.file.name,
        type: i.file.type,
        size: i.file.size,
        uploadPending: !!i.uploading
      } : null
    }));
  }

  /**
   * Retorna los datos listos para enviar al backend en AdminProductoDto.
   * Solo incluye imágenes con URLs HTTPS confirmadas y sin errores ni blobs.
   */
  getPayload(): Array<{ url: string; textoAlternativo: string; posicion: number; principal: boolean }> {
    return this.images()
      .filter(i => i.url && !i.uploading && !i.error && !i.previewError && !i.checkingPreview && i.url.startsWith('https://'))
      .map((i, index) => ({
        url: i.url,
        textoAlternativo: i.alt || '',
        posicion: index,
        principal: index === 0
      }));
  }

  ngOnDestroy(): void {
    this.destroyed = true; this.generation++;
    for (const sub of this.uploadSubs.values()) {
      sub.unsubscribe();
    }
    this.cleanupStagedUploads();
    for (const url of this.decodingUrls) URL.revokeObjectURL(url);
    for (const item of this.images()) {
      if (item.file && item.url.startsWith('blob:')) {
        URL.revokeObjectURL(item.url);
      }
    }
  }
}
