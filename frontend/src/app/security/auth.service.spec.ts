// @vitest-environment jsdom
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();
    service = TestBed.inject(AuthService);
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isAuthenticated retorna false quando não há role no localStorage', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('isAuthenticated retorna true quando role está no localStorage', () => {
    localStorage.setItem('role', 'PATIENT');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('getUserRole retorna null quando não há role', () => {
    expect(service.getUserRole()).toBeNull();
  });

  it('getUserRole retorna o valor do localStorage', () => {
    localStorage.setItem('role', 'PROFESSIONAL');
    expect(service.getUserRole()).toBe('PROFESSIONAL');
  });

  it('isVerified retorna false quando verified não está definido', () => {
    expect(service.isVerified()).toBe(false);
  });

  it('isVerified retorna true quando verified="true" no localStorage', () => {
    localStorage.setItem('verified', 'true');
    expect(service.isVerified()).toBe(true);
  });

  it('isVerified retorna false quando verified="false" no localStorage', () => {
    localStorage.setItem('verified', 'false');
    expect(service.isVerified()).toBe(false);
  });

  it('clearLocalSession remove role e verified do localStorage', () => {
    localStorage.setItem('role', 'ADMIN');
    localStorage.setItem('verified', 'true');
    service.clearLocalSession();
    expect(localStorage.getItem('role')).toBeNull();
    expect(localStorage.getItem('verified')).toBeNull();
  });
});
