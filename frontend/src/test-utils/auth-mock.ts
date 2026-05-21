/**
 * Mocks reutilizáveis do AuthService para testes Vitest.
 * Uso: providers: [{ provide: AuthService, useValue: mockAuthServiceFor('PROFESSIONAL') }]
 */
import { signal } from '@angular/core';

export function mockAuthServiceFor(role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT', email = 'test@test.com') {
  return {
    getUserRole: () => role,
    getUserEmail: () => email,
    isAuthenticated: () => true,
    login: () => Promise.resolve(),
    logout: () => {},
    getToken: () => 'fake-token',
    isVerified: () => true,
  };
}

export const mockAuthServiceAdmin       = mockAuthServiceFor('ADMIN', 'admin@test.com');
export const mockAuthServiceProfessional = mockAuthServiceFor('PROFESSIONAL', 'prof@test.com');
export const mockAuthServicePatient     = mockAuthServiceFor('PATIENT', 'paciente@test.com');

/** Mock mínimo do Router do Angular */
export const mockRouter = {
  navigate: () => Promise.resolve(true),
  navigateByUrl: () => Promise.resolve(true),
  events: signal([]).asReadonly(),
};
