// @vitest-environment jsdom
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { roleGuard } from './role.guard';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('roleGuard', () => {
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();
    router = TestBed.inject(Router);
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  it('retorna true quando a role do usuário está na lista permitida', () => {
    localStorage.setItem('role', 'ADMIN');
    const result = TestBed.runInInjectionContext(() => roleGuard(['ADMIN'])({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('retorna false e redireciona pro dashboard quando a role não está na lista permitida', () => {
    localStorage.setItem('role', 'PATIENT');
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const result = TestBed.runInInjectionContext(() => roleGuard(['ADMIN'])({} as any, {} as any));
    expect(result).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/panel/dashboard']);
  });

  it('retorna false e redireciona quando não há role (não autenticado)', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const result = TestBed.runInInjectionContext(() => roleGuard(['PROFESSIONAL', 'PATIENT'])({} as any, {} as any));
    expect(result).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/panel/dashboard']);
  });

  it('aceita mais de uma role permitida', () => {
    localStorage.setItem('role', 'PROFESSIONAL');
    const result = TestBed.runInInjectionContext(() => roleGuard(['PROFESSIONAL', 'PATIENT'])({} as any, {} as any));
    expect(result).toBe(true);
  });
});
