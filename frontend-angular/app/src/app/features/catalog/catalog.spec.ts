import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Catalog } from './catalog';
import { CatalogService } from './catalog.service';
import { CartService } from '../../core/cart/cart.service';
import { FavoritesService } from '../../core/favorites/favorites.service';
import { CatalogPage } from './catalog.models';

registerLocaleData(localeEsPe);

describe('Navegación del catálogo', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  const path = new BehaviorSubject(convertToParamMap({}));
  const product = { id: 'cpu-1', sku: 'CPU', slug: 'cpu', name: 'CPU de prueba', brand: 'AMD', category: 'Procesadores', price: 100, currency: 'PEN' as const, availableStock: 0, primaryImage: null };
  const page: CatalogPage = { items: [product], page: 0, size: 12, totalItems: 17, totalPages: 2 };
  const service = { getProducts: vi.fn(), getFilters: vi.fn(), getCategories: vi.fn() };

  beforeEach(() => {
    path.next(convertToParamMap({}));
    service.getCategories.mockReset().mockReturnValue(of([{nombre:'Mouse',slug:'mouse'},{nombre:'Mousepads',slug:'mousepads'}]));
    params.next(convertToParamMap({ categoria: 'procesadores', marca: 'amd', 'filtro.socket_cpu': 'AM5' }));
    service.getProducts.mockReset().mockReturnValue(of(page));
    service.getFilters.mockReset().mockReturnValue(of({ groups: [
      { code: '__hardware', label: 'Hardware', kind: 'section', options: [{ value: 'procesadores', label: 'Procesadores', count: 17 }] },
      { code: '__marca', label: 'Fabricante', kind: 'filter', options: [{ value: 'amd', label: 'AMD', count: 12 }] },
      { code: 'socket_cpu', label: 'Socket', kind: 'filter', options: [{ value: 'AM5', label: 'AM5', count: 4 }] },
    ] }));
    TestBed.configureTestingModule({ imports: [Catalog], providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { queryParamMap: params, paramMap:path } },
      { provide: CatalogService, useValue: service },
      { provide: CartService, useValue: {} },
      { provide: FavoritesService, useValue: { has: () => false, toggle: vi.fn() } },
    ] });
  });

  it('los ocho iconos enlazan a páginas propias sin búsquedas de texto',()=>{
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();
    const links=[...(fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('a.category-bubble-item')];
    expect(links).toHaveLength(8);
    expect(links.map(link=>link.getAttribute('href'))).toContain('/catalogo/mouse-gaming');
    expect(links.map(link=>link.getAttribute('href'))).toContain('/catalogo/mousepads-gaming');
    expect(links.every(link=>!link.href.includes('?'))).toBe(true);
  });

  it('cada botón rápido usa las mismas opciones y selección múltiple que Todos los filtros',()=>{
    path.next(convertToParamMap({categoria:'mouse-gaming'}));params.next(convertToParamMap({}));
    service.getFilters.mockReturnValue(of({groups:[
      {code:'CATALOG_TECH_MOUSE_COLOR',label:'Color',kind:'filter',options:[{value:'Negro',label:'Negro',count:0},{value:'Blanco',label:'Blanco',count:0}]},
      {code:'CATALOG_TECH_MOUSE_SENSOR',label:'Sensor',kind:'filter',options:[{value:'Óptico',label:'Óptico',count:0}]},
      {code:'empty',label:'No disponible',kind:'filter',options:[]},
    ]}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();const component=fixture.componentInstance as any;
    const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    const buttons=fixture.nativeElement.querySelectorAll('.quick-filter-trigger');expect(buttons.length).toBe(2);
    buttons[0].click();fixture.detectChanges();
    const checks=fixture.nativeElement.querySelectorAll('.quick-filter-options input');expect(checks.length).toBe(2);
    for(const check of checks){check.checked=true;check.dispatchEvent(new Event('change'));}
    component.applyQuickFilter();
    expect(navigate).toHaveBeenLastCalledWith(['/catalogo','mouse-gaming'],{queryParams:{'filtro.CATALOG_TECH_MOUSE_COLOR':['Negro','Blanco']}});
    params.next(convertToParamMap({'filtro.CATALOG_TECH_MOUSE_COLOR':['Negro','Blanco']}));
    component.openFilters();fixture.detectChanges();
    const drawer=fixture.nativeElement.querySelector('.filter-drawer');
    expect(drawer.textContent).toContain('Color');expect(drawer.querySelectorAll('input:checked').length).toBe(2);
  });

  it('cambia los botones al cambiar la categoría y descarta una selección rápida no aplicada',()=>{
    service.getCategories.mockReturnValue(of([{nombre:'Mouse',slug:'mouse'},{nombre:'Teclados',slug:'teclados'}]));
    service.getFilters.mockImplementation((_q:string,category:string)=>of({groups:[{code:category==='mouse'?'sensor':'layout',label:category==='mouse'?'Sensor':'Distribución',kind:'filter',options:[{value:'dato',label:'Dato real',count:0}]}]}));
    path.next(convertToParamMap({categoria:'mouse-gaming'}));params.next(convertToParamMap({}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();const component=fixture.componentInstance as any;
    component.openQuickFilter('sensor');component.toggleFilter('sensor','dato',true);component.openFilters();
    expect(component.selectedFilters()['sensor']).toBeUndefined();
    path.next(convertToParamMap({categoria:'teclados-gaming'}));fixture.detectChanges();
    const toolbar=fixture.nativeElement.querySelector('.catalog-filter-actions');expect(toolbar.textContent).toContain('Distribución');expect(toolbar.textContent).not.toContain('Sensor');
  });

  it('abre una categoría con título propio y consulta el slug real sin usar el nombre como búsqueda',()=>{
    service.getCategories.mockReturnValue(of([{nombre:'Mouse gaming',slug:'mouse-gaming'},{nombre:'Mouse',slug:'mouse'}]));
    path.next(convertToParamMap({categoria:'mouse-gaming'}));params.next(convertToParamMap({}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Mouse');
    expect(service.getProducts).toHaveBeenCalledWith('',0,12,{__categoria:['mouse']},'nombre','asc');
    expect(service.getFilters).toHaveBeenCalledWith('','mouse');
    expect(fixture.nativeElement.querySelector('.catalog-filter-actions #catalog-sort')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.category-bubbles-section')).toBeNull();
    expect(fixture.nativeElement.querySelector('.category-heading-icon')).toBeNull();
    const filters=fixture.nativeElement.querySelector('.catalog-filter-actions');
    expect(filters.compareDocumentPosition(fixture.nativeElement.querySelector('.catalog-grid')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    const component=fixture.componentInstance as any;component.openFilters();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.filter-drawer').textContent).not.toContain('Elige una sección');
    expect(fixture.nativeElement.querySelector('.filter-drawer').textContent).not.toContain('Procesadores');
    component.toggleFilter('__hardware','procesadores',true);
    expect(component.selectedFilters()['__categoria']).toEqual(['mouse']);
  });

  it('muestra el vacío de la categoría incluso si todavía no está registrada, sin cargar todo el catálogo',()=>{
    path.next(convertToParamMap({categoria:'microfonos'}));params.next(convertToParamMap({}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();
    expect(service.getProducts).not.toHaveBeenCalled();expect(service.getFilters).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Por ahora no hay productos de Micrófonos');
    expect(fixture.nativeElement.textContent).toContain('Ver todo el catálogo');
  });

  it('mantiene la página de categoría al paginar, ordenar y limpiar filtros',()=>{
    path.next(convertToParamMap({categoria:'mousepads-gaming'}));params.next(convertToParamMap({marca:'marca-1'}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();const component=fixture.componentInstance as any;
    const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);
    component.goToPage(1);expect(navigate).toHaveBeenLastCalledWith(['/catalogo','mousepads-gaming'],{queryParams:{marca:['marca-1'],pagina:2}});
    component.changeSort('precio-desc');expect(navigate).toHaveBeenLastCalledWith(['/catalogo','mousepads-gaming'],{queryParams:{marca:['marca-1'],orden:'precio-desc'}});
    component.clearFilters();expect(navigate).toHaveBeenLastCalledWith(['/catalogo','mousepads-gaming'],{queryParams:{}});
  });

  it('no confunde un error al consultar categorías con ausencia de productos y permite reintentar',()=>{
    path.next(convertToParamMap({categoria:'mouse-gaming'}));params.next(convertToParamMap({}));
    service.getCategories.mockReturnValueOnce(throwError(()=>new Error('offline')));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos consultar el catálogo');
    (fixture.componentInstance as any).load();fixture.detectChanges();
    expect(service.getProducts).toHaveBeenCalledWith('',0,12,{__categoria:['mouse']},'nombre','asc');
  });

  it('cancela consultas pendientes al pasar entre páginas de categoría',()=>{
    const old=new Subject<CatalogPage>();service.getProducts.mockReturnValueOnce(old).mockReturnValue(of(page));
    path.next(convertToParamMap({categoria:'mouse-gaming'}));params.next(convertToParamMap({}));
    const fixture=TestBed.createComponent(Catalog);fixture.detectChanges();expect(old.observed).toBe(true);
    path.next(convertToParamMap({categoria:'mousepads-gaming'}));fixture.detectChanges();
    expect(old.observed).toBe(false);expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Mousepads para gaming');
    expect(service.getProducts).toHaveBeenLastCalledWith('',0,12,{__categoria:['mousepads']},'nombre','asc');
  });

  it('sitúa el botón de filtros después del carrusel y antes del contador', () => {
    const fixture = TestBed.createComponent(Catalog); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const carousel = element.querySelector('.category-bubbles-section')!;
    const filters = element.querySelector('.catalog-filter-actions')!;
    const results = element.querySelector('#catalog-results')!;
    expect(carousel.compareDocumentPosition(filters) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(filters.compareDocumentPosition(results) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(element.querySelector('.page-heading .filter-trigger')).toBeNull();
  });

  it('pagina manteniendo categoría, marca y atributos', () => {
    const fixture = TestBed.createComponent(Catalog);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    const next = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find(button => button.textContent?.trim() === 'Siguiente')!;
    next.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: {
      categoria: ['procesadores'], marca: ['amd'], 'filtro.socket_cpu': ['AM5'], pagina: 2,
    } }));
  });

  it('ordena desde la API y consulta la página indicada en la URL', () => {
    params.next(convertToParamMap({ pagina: '2', orden: 'precio-desc' }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(service.getProducts).toHaveBeenCalledWith('', 1, 12, {}, 'precio', 'desc');
    expect((fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>('#catalog-sort')!.value).toBe('precio-desc');
  });

  it('muestra chips legibles y permite eliminar un atributo sin quitar los demás', () => {
    const fixture = TestBed.createComponent(Catalog);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    const chip = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[aria-label="Quitar filtro Socket: AM5"]')!;
    expect(chip).toBeTruthy();
    chip.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { categoria: ['procesadores'], marca: ['amd'] } }));
  });

  it('muestra un estado vacío con una acción para limpiar la búsqueda', () => {
    service.getProducts.mockReturnValue(of({ ...page, items: [], totalItems: 0, totalPages: 0 }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No encontramos resultados');
    expect(fixture.nativeElement.textContent).toContain('Limpiar búsqueda y filtros');
  });

  it('cancela consultas anteriores al cambiar la búsqueda', () => {
    const previous = new Subject<CatalogPage>();
    service.getProducts.mockReturnValueOnce(previous).mockReturnValue(of(page));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(previous.observed).toBe(true);
    params.next(convertToParamMap({ q: 'nuevo' }));
    expect(previous.observed).toBe(false);
  });

  it('sustituye imágenes rotas por el marcador de su categoría', () => {
    service.getProducts.mockReturnValue(of({ ...page, items: [{ ...product, primaryImage: { url: '/imagen-inexistente.png', alt: null } }] }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    const image = (fixture.nativeElement as HTMLElement).querySelector('.product-image img')!;
    image.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.product-image img')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.image-unavailable')?.textContent).toContain('Procesadores');
  });
});
