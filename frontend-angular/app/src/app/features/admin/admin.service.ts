import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, throwError } from 'rxjs';
import { ADMIN_MODULES } from './admin-modules';

export interface AdminRow {
  id: string;
  code: string;
  title: string;
  subtitle?: string;
  tag?: string;
  detail?: string;
  metric?: string;
  status: 'active' | 'draft' | 'inactive' | 'alert' | 'pending';
  statusText: string;
  thumbnail?: string;
  rawData?: any;
}

export interface AdminPagedResult {
  items: AdminRow[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminStats {
  totalProductos: number | null;
  pedidosPendientes: number | null;
  stockBajo: number | null;
  fichasPorVerificar: number | null;
  categoriasActivas: number | null;
}

export interface AdminMetadata {
  categorias: Array<{ id: string; nombre: string; slug: string }>;
  marcas: Array<{ id: string; nombre: string; slug: string }>;
}

export interface AlmacenItem {
  id: string;
  nombre: string;
  codigo: string;
  direccion?: string;
  activo: boolean;
}

export interface InventarioItem {
  varianteId?: string;
  version: number;
  existenciaId: string;
  productoId: string;
  productoNombre: string;
  productoSku: string;
  productoSlug?: string;
  marca?: string;
  categoria?: string;
  imagenPrincipal?: string;
  almacenId: string;
  almacenNombre: string;
  almacenCodigo: string;
  cantidad: number;
  reservado: number;
  disponible: number;
  estadoAlerta: string;
}

export interface AjusteStockPayload {
  varianteId?: string;
  operacionId: string;
  versionEsperada: number;
  existenciaId?: string;
  productoId: string;
  almacenId: string;
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE';
  cantidad: number;
  nota: string;
}

export interface MovimientoInventario {
  actor?: string;
  cantidadAnterior?: number;
  cantidadNueva?: number;
  delta?: number;
  id: string;
  existenciaId: string;
  tipo: string;
  cantidad: number;
  referenciaTipo?: string;
  referenciaId?: string;
  nota?: string;
  registradoAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  // Almacén aislado en memoria, exclusivo de la vista de diseño.
  private readonly localStore = new Map<string, AdminRow[]>();

  constructor() {
    this.seedFallbackData();
  }

  /**
   * Obtiene estadísticas reales para el Centro de Control
   */
  getStats(): Observable<AdminStats> {
    return forkJoin({
      productos: this.http.get<any>('/api/v1/admin/productos?size=1').pipe(catchError(() => of(null))),
      alertas: this.http.get<any>('/api/v1/admin/productos?estado=alert&size=1').pipe(catchError(() => of(null))),
      categorias: this.http.get<any[]>('/api/v1/categorias').pipe(catchError(() => of(null))),
    }).pipe(
      map(res => ({
        totalProductos: res.productos?.totalElements !== undefined ? Number(res.productos.totalElements) : null,
        pedidosPendientes: null,
        stockBajo: res.alertas?.totalElements !== undefined ? Number(res.alertas.totalElements) : null,
        fichasPorVerificar: null,
        categoriasActivas: Array.isArray(res.categorias) ? res.categorias.length : null,
      })),
      catchError(() => of({
        totalProductos: null,
        pedidosPendientes: null,
        stockBajo: null,
        fichasPorVerificar: null,
        categoriasActivas: null,
      }))
    );
  }

  /**
   * Obtiene metadatos de categorías y marcas para selectores dinámicos
   */
  getMetadata(): Observable<AdminMetadata> {
    return this.http.get<AdminMetadata>('/api/v1/admin/productos/metadata');
  }

  isModuleConnected(moduleId: string): boolean { return ['catalogo', 'inventario', 'contenido'].includes(moduleId); }

  getPreviewItems(moduleId: string, query = '', page = 0, pageSize = 10, status = ''): Observable<AdminPagedResult> {
    return this.getLocalItems(moduleId, query, page, pageSize, status);
  }

  getPreviewStats(): Observable<AdminStats> {
    return of({ totalProductos: (this.localStore.get('catalogo') ?? []).length, pedidosPendientes: null, stockBajo: null, fichasPorVerificar: null, categoriasActivas: null });
  }

  /**
   * Registra una nueva categoría en el backend
   */
  createCategory(data: { nombre: string; acronimo?: string; slug?: string; parentId?: string }): Observable<{ id: string; nombre: string; slug: string }> {
    return this.http.post<any>('/api/v1/admin/productos/categorias', data).pipe(
      map(res => ({ id: res.id, nombre: res.nombre, slug: res.slug }))
    );
  }

  /**
   * Registra una nueva marca en el backend
   */
  createBrand(data: { nombre: string; slug?: string; logoUrl?: string }): Observable<{ id: string; nombre: string; slug: string }> {
    return this.http.post<any>('/api/v1/admin/productos/marcas', data).pipe(
      map(res => ({ id: res.id, nombre: res.nombre, slug: res.slug }))
    );
  }

  /**
   * Obtiene listado paginado y filtrado para cualquier módulo administrativo
   */
  getItems(moduleId: string, query = '', page = 0, pageSize = 10, statusFilter = ''): Observable<AdminPagedResult> {
    if (moduleId === 'catalogo') {
      return this.getCatalogItems(query, page, pageSize, statusFilter);
    }
    if (moduleId === 'inventario') {
      return this.getInventoryReal(query, page, pageSize, statusFilter);
    }
    if (moduleId === 'contenido') {
      return this.getContentItems(query, page, pageSize, statusFilter);
    }

    // Para módulos con datos locales o en preparación backend:
    return of({ items: [], total: 0, page: 0, pageSize, totalPages: 0 });
  }

  /**
   * Mapea un producto del backend a la estructura AdminRow
   */
  private mapProductToRow(p: any): AdminRow {
    const stock = p.stockDisponible ?? 0;
    const isActivo = p.activo !== false;
    const isVendible = p.vendible !== false;
    let status: AdminRow['status'] = 'active';
    let statusText = 'Activo';

    if (!isActivo || !isVendible) {
      status = 'draft';
      statusText = 'No publicado';
    } else if (stock <= 0) {
      status = 'alert';
      statusText = 'Agotado';
    } else if (stock <= 3) {
      status = 'alert';
      statusText = 'Bajo stock';
    }

    return {
      id: p.id,
      code: p.sku || 'SKU-PENDIENTE',
      title: p.nombre,
      subtitle: p.marca || 'Sin marca',
      tag: p.categoria || 'Sin categoría',
      detail: `${stock} un. disponibles`,
      metric: p.precioBase == null ? 'Precio sin configurar' : `S/ ${Number(p.precioBase).toFixed(2)}`,
      status,
      statusText,
      thumbnail: p.imagenPrincipal || undefined,
      rawData: p,
    };
  }

  /**
   * Consulta real al endpoint administrativo de productos con filtros y paginación en servidor
   */
  private getCatalogItems(query: string, page: number, pageSize: number, statusFilter: string): Observable<AdminPagedResult> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString())
      .set('sortBy', 'nombre')
      .set('sortDir', 'asc');

    if (query.trim()) {
      params = params.set('busqueda', query.trim());
    }
    if (statusFilter) {
      params = params.set('estado', statusFilter);
    }

    return this.http.get<any>('/api/v1/admin/productos', { params }).pipe(
      map(res => {
        const products = res?.content ?? [];
        const items: AdminRow[] = products.map((p: any) => this.mapProductToRow(p));

        return {
          items,
          total: res?.totalElements ?? items.length,
          page: res?.page ?? page,
          pageSize: res?.size ?? pageSize,
          totalPages: res?.totalPages ?? (Math.ceil((res?.totalElements ?? items.length) / pageSize) || 1),
        };
      })
    );
  }

  /**
   * Obtiene la lista de almacenes disponibles
   */
  getAlmacenes(): Observable<AlmacenItem[]> {
    return this.http.get<AlmacenItem[]>('/api/v1/admin/inventario/almacenes');
  }

  /**
   * Registra un movimiento o ajuste de inventario (ENTRADA, SALIDA, AJUSTE) con justificación
   */
  ajustarStock(payload: AjusteStockPayload): Observable<InventarioItem> {
    if (!payload.productoId) return throwError(() => new Error('El producto es obligatorio.'));
    if (!payload.almacenId) return throwError(() => new Error('El almacén es obligatorio.'));
    if (!['ENTRADA', 'SALIDA', 'AJUSTE'].includes(payload.tipo)) {
      return throwError(() => new Error('El tipo de movimiento debe ser ENTRADA, SALIDA o AJUSTE.'));
    }
    if (payload.cantidad == null || payload.cantidad < 0 || !Number.isInteger(payload.cantidad)) {
      return throwError(() => new Error('La cantidad debe ser un número entero mayor o igual a 0.'));
    }
    if (!payload.nota || !payload.nota.trim()) {
      return throwError(() => new Error('El motivo o justificación del ajuste es obligatorio para la trazabilidad.'));
    }
    return this.http.post<InventarioItem>('/api/v1/admin/inventario/ajuste', payload);
  }

  /**
   * Consulta el historial de movimientos (Kardex) de una existencia
   */
  getKardex(existenciaId: string, page = 0): Observable<{content: MovimientoInventario[]; totalPages: number}> {
    return this.http.get<{content: MovimientoInventario[]; totalPages: number}>(`/api/v1/admin/inventario/${existenciaId}/kardex`, {params: {page, size: 20}});
  }

  consultarStock(productoId: string, almacenId: string, varianteId?: string): Observable<InventarioItem> {
    let params = new HttpParams().set('productoId', productoId).set('almacenId', almacenId);
    if (varianteId) params = params.set('varianteId', varianteId);
    return this.http.get<InventarioItem>('/api/v1/admin/inventario/stock', {params});
  }

  /**
   * Consulta real de existencias e inventario desde el endpoint administrativo
   */
  getInventoryReal(query = '', page = 0, pageSize = 10, statusFilter = ''): Observable<AdminPagedResult> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());

    if (query.trim()) {
      params = params.set('busqueda', query.trim());
    }
    if (statusFilter) {
      params = params.set('alerta', statusFilter);
    }

    return this.http.get<any>('/api/v1/admin/inventario', { params }).pipe(
      map(res => {
        const content = res?.content ?? [];
        const items: AdminRow[] = content.map((item: InventarioItem) => {
          if (![item.disponible,item.reservado,item.cantidad].every(n => Number.isInteger(n) && n >= 0)
            || item.disponible !== item.cantidad - item.reservado) throw new Error('El servidor devolvió existencias incompletas o inconsistentes.');
          const disponible = item.disponible;
          const reservado = item.reservado;
          const total = item.cantidad;
          let status: AdminRow['status'] = 'active';
          let statusText = 'Disponible';

          if (disponible <= 0) {
            status = 'alert';
            statusText = 'Agotado';
          } else if (disponible <= 3) {
            status = 'alert';
            statusText = 'Bajo stock';
          }

          return {
            id: item.existenciaId,
            code: item.productoSku || 'SKU-PENDIENTE',
            title: item.productoNombre,
            subtitle: `${disponible} un. disponibles`,
            tag: item.almacenNombre || 'Almacén Central',
            detail: `Reservado: ${reservado} un. | Total: ${total} un.`,
            metric: `Físico: ${total}`,
            status,
            statusText,
            thumbnail: item.imagenPrincipal,
            rawData: item,
          };
        });

        return {
          items,
          total: res?.totalElements ?? items.length,
          page: res?.page ?? page,
          pageSize: res?.size ?? pageSize,
          totalPages: res?.totalPages ?? 1,
        };
      })
    );
  }

  /**
   * Consulta real de contenido y preguntas frecuentes
   */
  private getContentItems(query: string, page: number, pageSize: number, statusFilter: string): Observable<AdminPagedResult> {
    const params = new HttpParams().set('q', query.trim()).set('page', page).set('size', pageSize);
    if (statusFilter && statusFilter !== 'active') return of({items:[],total:0,page:0,pageSize,totalPages:0});
    return this.http.get<any>('/api/v1/ayuda/preguntas', {params}).pipe(
      map(res => {
        const questions = res?.content ?? [];
        const items: AdminRow[] = questions.map((q: any) => ({
          id: q.id,
          code: `FAQ-${q.id}`,
          title: q.pregunta,
          subtitle: q.categoria || 'General',
          tag: 'Pregunta frecuente',
          detail: q.respuesta ? q.respuesta.slice(0, 80) + '...' : '',
          metric: 'Público',
          status: 'active',
          statusText: 'Publicado',
          rawData: q,
        }));

        return {items,total:res.totalElements,page:res.page,pageSize:res.size,totalPages:res.totalPages};
      })
    );
  }

  /**
   * Guarda o actualiza un registro y emite respuesta del servidor
   */
  saveItem(moduleId: string, data: any, isEdit: boolean): Observable<{ success: boolean; message: string; row: AdminRow }> {
    if (moduleId === 'catalogo') {
      if (data.precio == null || data.precio === '' || !Number.isFinite(Number(data.precio)) || Number(data.precio) < 0) {
        return throwError(() => new Error('Ingresa un precio válido; un campo vacío no equivale a cero.'));
      }
      if (isEdit && !data.id) return throwError(() => new Error('Falta el identificador del producto que deseas editar.'));
      const payload: any = {
        nombre: data.nombre?.trim(),
        sku: data.sku?.trim(),
        slug: data.slug?.trim() || undefined,
        categoriaId: data.categoriaId || undefined,
        categoriaNombre: data.categoria?.trim() || undefined,
        marcaId: data.marcaId || undefined,
        marcaNombre: data.marca?.trim() || undefined,
        precioBase: Number(data.precio),
        costoReferencia: data.costoReferencia !== undefined && data.costoReferencia !== null && data.costoReferencia !== ''
          ? Number(data.costoReferencia)
          : undefined,
        estado: data.estado || 'active',
        activo: data.estado === 'active',
        vendible: data.estado === 'active',
        requiereSerie: data.requiereSerie,
        requiereEnvio: data.requiereEnvio,
        descripcion: data.descripcion !== undefined ? data.descripcion.trim() : undefined,
        imagenPrincipalUrl: data.imagenPrincipalUrl?.trim() || (data.imagenes?.[0]?.url) || undefined,
        imagenes: data.imagenes !== undefined
          ? data.imagenes.map((img: any, idx: number) => ({
              url: img.url,
              textoAlternativo: img.textoAlternativo || img.alt || data.nombre || '',
              posicion: img.posicion !== undefined ? img.posicion : (img.order !== undefined ? img.order - 1 : idx),
              principal: img.principal !== undefined ? img.principal : (img.primary !== undefined ? img.primary : (idx === 0))
            }))
          : undefined,
      };

      if (!isEdit) {
        payload.stockInicial = Number(data.stockInicial ?? 0);
      }

      const sync$ = isEdit && data.id
        ? this.http.put<any>(`/api/v1/admin/productos/${data.id}`, payload)
        : this.http.post<any>('/api/v1/admin/productos', payload);

      return sync$.pipe(
        map(savedProduct => {
          const row = this.mapProductToRow(savedProduct);
          const actionText = isEdit ? 'actualizado' : 'creado y guardado';
          return {
            success: true,
            message: `El registro "${row.title}" fue ${actionText} con éxito en el servidor.`,
            row,
          };
        })
      );
    }

    return throwError(() => new Error('La gestión de esta sección todavía no está conectada.'));
  }

  /**
   * Publicar producto directamente
   */
  publishProduct(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.patch<any>(`/api/v1/admin/productos/${id}/publicar`, {}).pipe(
      map(() => ({ success: true, message: 'Producto publicado en la tienda con éxito.' }))
    );
  }

  /**
   * Desactivar producto (oculta sin borrar historial)
   */
  deactivateProduct(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.patch<any>(`/api/v1/admin/productos/${id}/desactivar`, {}).pipe(
      map(() => ({ success: true, message: 'Producto desactivado (oculto de la tienda sin perder historial).' }))
    );
  }

  /**
   * Guardar producto como borrador
   */
  saveDraftProduct(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.patch<any>(`/api/v1/admin/productos/${id}/borrador`, {}).pipe(
      map(() => ({ success: true, message: 'Producto guardado en borrador (no vendible).' }))
    );
  }

  /**
   * Obtiene el detalle completo de un producto para edición (incluyendo galería completa de imágenes)
   */
  getProductDetail(id: string): Observable<any> {
    return this.http.get<any>(`/api/v1/admin/productos/${id}`);
  }

  /**
   * Eliminar producto (soft delete)
   */
  deleteProduct(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.delete<void>(`/api/v1/admin/productos/${id}`).pipe(
      map(() => ({ success: true, message: 'Producto eliminado correctamente del catálogo.' }))
    );
  }

  /**
   * Alterna el estado activo/inactivo de un elemento
   */
  toggleStatus(moduleId: string, id: string): Observable<{ success: boolean; message: string; newStatus: AdminRow['status'] }> {
    if (moduleId !== 'catalogo') return throwError(() => new Error('La gestión de esta sección todavía no está conectada.'));
    const list = this.localStore.get(moduleId) || [];
    const item = list.find(it => it.id === id);
    let newStatus: AdminRow['status'] = 'inactive';

    if (item) {
      newStatus = item.status === 'active' ? 'inactive' : 'active';
      item.status = newStatus;
      item.statusText = newStatus === 'active' ? 'Activo' : 'Inactivo';
    }

    if (moduleId === 'catalogo') {
      const endpoint = newStatus === 'active' ? 'publicar' : 'desactivar';
      return this.http.patch<any>(`/api/v1/admin/productos/${id}/${endpoint}`, {}).pipe(
        map(() => ({
          success: true,
          message: `El estado del registro fue cambiado a "${newStatus === 'active' ? 'Activo' : 'Inactivo'}".`,
          newStatus,
        }))
      );
    }

    return of({
      success: true,
      message: `El estado del registro fue cambiado a "${newStatus === 'active' ? 'Activo' : 'Inactivo'}".`,
      newStatus,
    });
  }

  /**
   * Elimina un registro
   */
  deleteItem(moduleId: string, id: string): Observable<{ success: boolean; message: string }> {
    if (moduleId === 'catalogo') {
      return this.deleteProduct(id);
    }
    return throwError(() => new Error('La gestión de esta sección todavía no está conectada.'));
  }

  /**
   * Guardado local en memoria (aislado para demostración o modo diseño)
   */
  saveItemLocally(moduleId: string, data: any, isEdit: boolean): Observable<{ success: boolean; message: string; row: AdminRow }> {
    const list = this.localStore.get(moduleId) || [];
    const id = isEdit && data.id ? data.id : `REC-${Date.now().toString().slice(-6)}`;
    const code = data.sku || data.codigo || `COD-${Math.floor(1000 + Math.random() * 9000)}`;

    const row: AdminRow = {
      id,
      code,
      title: data.nombre || data.titulo || data.producto || 'Nuevo registro',
      subtitle: data.marca || data.cliente || data.autor || '',
      tag: data.categoria || data.tipo || 'General',
      detail: data.descripcion || data.detalle || `${data.stockInicial ?? data.cantidad ?? 0} unidades`,
      metric: data.precio ? `S/ ${Number(data.precio).toFixed(2)}` : data.descuento ? `${data.descuento}%` : 'S/ 0.00',
      status: (data.estado as any) || 'active',
      statusText: (data.estado === 'draft' ? 'Borrador' : data.estado === 'inactive' ? 'Inactivo' : 'Activo'),
      thumbnail: data.imagenPrincipalUrl,
      rawData: data,
    };

    if (isEdit) {
      const index = list.findIndex(item => item.id === id);
      if (index !== -1) {
        list[index] = row;
      } else {
        list.unshift(row);
      }
    } else {
      list.unshift(row);
    }

    this.localStore.set(moduleId, list);

    const actionText = isEdit ? 'actualizado' : (data.estado === 'draft' ? 'guardado en borrador' : 'creado y guardado');
    return of({
      success: true,
      message: `El registro "${row.title}" fue ${actionText} con éxito.`,
      row,
    });
  }

  updateLocalStatus(moduleId: string, id: string, status: AdminRow['status'], text: string): void {
    const list = this.localStore.get(moduleId) || [];
    const item = list.find(it => it.id === id);
    if (item) {
      item.status = status;
      item.statusText = text;
    }
  }

  deleteLocalItem(moduleId: string, id: string): void {
    const list = this.localStore.get(moduleId) || [];
    const index = list.findIndex(it => it.id === id);
    if (index !== -1) {
      list.splice(index, 1);
      this.localStore.set(moduleId, list);
    }
  }

  /**
   * Manejo de lista local con filtrado y paginación
   */
  private getLocalItems(moduleId: string, query: string, page: number, pageSize: number, statusFilter: string): Observable<AdminPagedResult> {
    const list = this.localStore.get(moduleId) || [];
    const filtered = this.filterList(list, query, statusFilter);
    return of(this.paginateList(filtered, page, pageSize));
  }

  private filterList(items: AdminRow[], query: string, statusFilter: string): AdminRow[] {
    const q = query.toLowerCase().trim();
    return items.filter(it => {
      const matchQuery = !q ||
        it.title.toLowerCase().includes(q) ||
        it.code.toLowerCase().includes(q) ||
        (it.subtitle && it.subtitle.toLowerCase().includes(q)) ||
        (it.tag && it.tag.toLowerCase().includes(q));

      const matchStatus = !statusFilter || it.status === statusFilter;
      return matchQuery && matchStatus;
    });
  }

  private paginateList(items: AdminRow[], page: number, pageSize: number): AdminPagedResult {
    const total = items.length;
    const totalPages = Math.ceil(total / pageSize);
    const currentPage = Math.max(0, Math.min(page, totalPages - 1));
    const start = currentPage * pageSize;
    const pagedItems = items.slice(start, start + pageSize);

    return {
      items: pagedItems,
      total,
      page: currentPage,
      pageSize,
      totalPages,
    };
  }

  /**
   * Inicialización de almacenes locales vacíos para módulos sin conectar
   */
  private seedFallbackData(): void {
    this.localStore.set('catalogo', []);
    this.localStore.set('pedidos', []);
    this.localStore.set('ofertas', []);
    this.localStore.set('clientes', []);
    this.localStore.set('resenas', []);
    this.localStore.set('compatibilidad', []);
    this.localStore.set('envios', []);
    this.localStore.set('postventa', []);
    this.localStore.set('calidad', []);
  }
}
