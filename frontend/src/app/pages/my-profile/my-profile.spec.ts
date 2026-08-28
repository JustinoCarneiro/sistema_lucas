// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyProfileComponent } from './my-profile';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('MyProfileComponent', () => {
  let component: MyProfileComponent;
  let fixture: ComponentFixture<MyProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyProfileComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(MyProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('profile começa como objeto vazio', () => {
    expect(component.profile()).toEqual({});
  });

  it('isLoading começa true enquanto aguarda dados do perfil', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('showPasswordModal começa fechado', () => {
    expect(component.showPasswordModal()).toBe(false);
  });

  it('updateProfile atualiza campo simples do perfil', () => {
    component.profile.set({ name: 'João' });
    component.updateProfile('name', 'Maria');
    expect(component.profile().name).toBe('Maria');
  });

  it('updateProfile aplica máscara de telefone automaticamente', () => {
    component.profile.set({});
    component.updateProfile('phone', '11999999999');
    expect(component.profile().phone).toBe('(11) 99999-9999');
  });

  it('updateProfile aplica máscara de CPF automaticamente', () => {
    component.profile.set({});
    component.updateProfile('cpf', '12345678901');
    expect(component.profile().cpf).toBe('123.456.789-01');
  });

  it('applyCpfMask formata CPF de 11 dígitos', () => {
    expect(component.applyCpfMask('12345678901')).toBe('123.456.789-01');
  });

  it('applyCpfMask formata CPF parcial corretamente', () => {
    expect(component.applyCpfMask('123456')).toBe('123.456');
    expect(component.applyCpfMask('123')).toBe('123');
    expect(component.applyCpfMask('')).toBe('');
  });

  it('applyPhoneMask formata celular de 11 dígitos', () => {
    expect(component.applyPhoneMask('11999999999')).toBe('(11) 99999-9999');
  });

  it('applyPhoneMask formata telefone fixo de 10 dígitos', () => {
    expect(component.applyPhoneMask('1133334444')).toBe('(11) 3333-4444');
  });

  it('applyPhoneMask retorna vazio para string vazia', () => {
    expect(component.applyPhoneMask('')).toBe('');
  });

  it('passwordMismatch retorna false quando senhas iguais', () => {
    component.modalNewPassword = 'abc123';
    component.modalConfirmPassword = 'abc123';
    expect(component.passwordMismatch()).toBe(false);
  });

  it('passwordMismatch retorna true quando senhas diferentes e confirmação preenchida', () => {
    component.modalNewPassword = 'abc123';
    component.modalConfirmPassword = 'xyz789';
    expect(component.passwordMismatch()).toBe(true);
  });

  it('passwordMismatch retorna false quando confirmação está vazia', () => {
    component.modalNewPassword = 'abc123';
    component.modalConfirmPassword = '';
    expect(component.passwordMismatch()).toBe(false);
  });

  it('isPasswordValid retorna false para senha com menos de 6 caracteres', () => {
    component.modalNewPassword = '123';
    component.modalConfirmPassword = '123';
    expect(component.isPasswordValid()).toBe(false);
  });

  it('isPasswordValid retorna true para senhas iguais com 6+ caracteres', () => {
    component.modalNewPassword = 'senha123';
    component.modalConfirmPassword = 'senha123';
    expect(component.isPasswordValid()).toBe(true);
  });

  it('openPasswordModal abre o modal e limpa campos', () => {
    component.modalNewPassword = 'antigo';
    component.modalConfirmPassword = 'antigo';
    component.openPasswordModal();
    expect(component.showPasswordModal()).toBe(true);
    expect(component.modalNewPassword).toBe('');
    expect(component.modalConfirmPassword).toBe('');
  });

  it('closePasswordModal fecha o modal', () => {
    component.showPasswordModal.set(true);
    component.closePasswordModal();
    expect(component.showPasswordModal()).toBe(false);
  });
});
