// @vitest-environment jsdom
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    http = TestBed.inject(HttpClient);
    httpController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpController.verify();
    localStorage.clear();
  });

  it('interceptor está registrado e permite que requests sejam feitas', () => {
    http.get('/test').subscribe();
    const req = httpController.expectOne('/test');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });

  it('request clonada inclui withCredentials=true', () => {
    http.get('/api/data').subscribe();
    const req = httpController.expectOne('/api/data');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });
});
