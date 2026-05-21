// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { vi, describe, beforeAll, beforeEach, it, expect } from 'vitest';
import { of, throwError } from 'rxjs';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;

  beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false, media: query, onchange: null,
        addListener: vi.fn(), removeListener: vi.fn(),
        addEventListener: vi.fn(), removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))
    });
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form inicialmente inválido com campos vazios', () => {
    expect(component.loginForm.invalid).toBe(true);
  });

  it('campo email inválido com formato incorreto', () => {
    component.loginForm.get('email')!.setValue('nao-e-email');
    expect(component.loginForm.get('email')!.invalid).toBe(true);
  });

  it('form válido com email e senha preenchidos corretamente', () => {
    component.loginForm.setValue({ email: 'user@test.com', password: '123456' });
    expect(component.loginForm.valid).toBe(true);
  });

  it('onSubmit com form inválido define errorMessage e não chama authService', () => {
    const loginSpy = vi.spyOn(authService, 'login');
    component.loginForm.setValue({ email: '', password: '' });
    component.onSubmit();
    expect(component.errorMessage()).toBe('Preencha os campos corretamente.');
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('onSubmit com form válido chama authService.login e limpa errorMessage', () => {
    vi.spyOn(authService, 'login').mockReturnValue(
      of({ role: 'PATIENT', verified: true }) as any
    );
    component.loginForm.setValue({ email: 'user@test.com', password: '123456' });
    component.onSubmit();
    expect(component.errorMessage()).toBe('');
  });

  it('onSubmit — erro do backend define errorMessage e isLoading=false', () => {
    vi.spyOn(authService, 'login').mockReturnValue(
      throwError(() => 'E-mail ou senha inválidos.')
    );
    component.loginForm.setValue({ email: 'user@test.com', password: 'errado' });
    component.onSubmit();
    expect(component.errorMessage()).toBe('E-mail ou senha inválidos.');
    expect(component.isLoading()).toBe(false);
  });
});
