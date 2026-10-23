import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { NotificationService } from '../notifications/notification.service';
import { mutationFeedbackInterceptor } from './mutation-feedback.interceptor';

describe('Errores globales de acciones', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [
    provideHttpClient(withInterceptors([mutationFeedbackInterceptor])), provideHttpClientTesting(),
  ] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it('avisa de un fallo sin reintentar una escritura ni esconder el error local', () => {
    let status = 0;
    TestBed.inject(HttpClient).post('/api/v1/carrito/items', {}).subscribe({ error: error => status = error.status });
    TestBed.inject(HttpTestingController).expectOne('/api/v1/carrito/items').flush({}, { status: 500, statusText: 'Error' });
    expect(status).toBe(500);
    expect(TestBed.inject(NotificationService).items()[0].kind).toBe('error');
    TestBed.inject(HttpTestingController).expectNone('/api/v1/carrito/items');
  });
  it('no duplica los avisos de consultas y sincronizaciones en segundo plano', () => {
    const http = TestBed.inject(HttpClient); const controller = TestBed.inject(HttpTestingController);
    http.get('/api/v1/productos').subscribe({ error: () => undefined });
    controller.expectOne('/api/v1/productos').flush({}, { status: 503, statusText: 'Offline' });
    http.post('/api/v1/carrito/vincular', {}).subscribe({ error: () => undefined });
    controller.expectOne('/api/v1/carrito/vincular').flush({}, { status: 503, statusText: 'Offline' });
    expect(TestBed.inject(NotificationService).items()).toEqual([]);
  });
});
