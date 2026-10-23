import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../../environments/environment';
import { apiUrlInterceptor } from './api-url.interceptor';

describe('apiUrlInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const originalApiUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiUrlInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.apiUrl = originalApiUrl;
    httpMock.verify();
  });

  it('mantiene la URL relativa cuando environment.apiUrl está vacía (modo dev / proxy local)', () => {
    environment.apiUrl = '';

    http.get('/api/v1/productos').subscribe();

    const req = httpMock.expectOne('/api/v1/productos');
    expect(req.request.url).toBe('/api/v1/productos');
    req.flush([]);
  });

  it('antepone la URL absoluta de Render cuando environment.apiUrl está configurada', () => {
    environment.apiUrl = 'https://rya-tech-backend.onrender.com';

    http.get('/api/v1/productos').subscribe();

    const req = httpMock.expectOne('https://rya-tech-backend.onrender.com/api/v1/productos');
    expect(req.request.url).toBe('https://rya-tech-backend.onrender.com/api/v1/productos');
    req.flush([]);
  });

  it('elimina las barras diagonales finales de environment.apiUrl para evitar dobles barras', () => {
    environment.apiUrl = 'https://rya-tech-backend.onrender.com///';

    http.get('/api/v1/pc-builder/inicial').subscribe();

    const req = httpMock.expectOne('https://rya-tech-backend.onrender.com/api/v1/pc-builder/inicial');
    expect(req.request.url).toBe('https://rya-tech-backend.onrender.com/api/v1/pc-builder/inicial');
    req.flush({});
  });

  it('no modifica peticiones a recursos que no comienzan con /api', () => {
    environment.apiUrl = 'https://rya-tech-backend.onrender.com';

    http.get('/assets/brand/group-1.svg').subscribe();

    const req = httpMock.expectOne('/assets/brand/group-1.svg');
    expect(req.request.url).toBe('/assets/brand/group-1.svg');
    req.flush('');
  });
});
