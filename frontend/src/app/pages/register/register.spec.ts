// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Register } from './register';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { throwError } from 'rxjs';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form inválido quando termsAccepted=false mesmo com restante preenchido', () => {
    component.registerForm.setValue({
      name: 'João', email: 'j@test.com', password: '123456',
      cpf: '12345678901', whatsapp: '11999999999', termsAccepted: false
    });
    expect(component.registerForm.invalid).toBe(true);
  });

  it('form válido quando todos os campos corretos e termsAccepted=true', () => {
    component.registerForm.setValue({
      name: 'João', email: 'j@test.com', password: '123456',
      cpf: '12345678901', whatsapp: '11999999999', termsAccepted: true
    });
    expect(component.registerForm.valid).toBe(true);
  });

  it('senha inválida com menos de 6 caracteres', () => {
    component.registerForm.get('password')!.setValue('123');
    expect(component.registerForm.get('password')!.invalid).toBe(true);
  });

  it('onSubmit com form inválido define errorMessage', () => {
    component.onSubmit();
    expect(component.errorMessage()).toBe('Preencha todos os campos corretamente.');
  });

  it('applyCpfMask formata CPF de 11 dígitos', () => {
    component.applyCpfMask({ target: { value: '12345678901' } });
    expect(component.registerForm.get('cpf')!.value).toBe('123.456.789-01');
  });

  it('applyCpfMask formata CPF parcial de 6 dígitos', () => {
    component.applyCpfMask({ target: { value: '123456' } });
    expect(component.registerForm.get('cpf')!.value).toBe('123.456');
  });

  it('applyPhoneMask formata celular de 11 dígitos', () => {
    component.applyPhoneMask({ target: { value: '11999999999' } });
    expect(component.registerForm.get('whatsapp')!.value).toBe('(11) 99999-9999');
  });

  it('applyPhoneMask formata telefone fixo de 10 dígitos', () => {
    component.applyPhoneMask({ target: { value: '1133334444' } });
    expect(component.registerForm.get('whatsapp')!.value).toBe('(11) 3333-4444');
  });

  it('onSubmit — erro do backend define errorMessage e isLoading=false', () => {
    const authService = TestBed.inject(AuthService);
    vi.spyOn(authService, 'registerPatient').mockReturnValue(
      throwError(() => 'CPF já cadastrado.')
    );
    component.registerForm.setValue({
      name: 'João', email: 'j@test.com', password: '123456',
      cpf: '12345678901', whatsapp: '11999999999', termsAccepted: true
    });
    component.onSubmit();
    expect(component.errorMessage()).toBe('CPF já cadastrado.');
    expect(component.isLoading()).toBe(false);
  });
});
