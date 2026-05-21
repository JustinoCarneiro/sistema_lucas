// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetPasswordComponent } from './reset-password';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';
import { FormGroup, FormControl } from '@angular/forms';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form inicialmente inválido', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('sucesso começa false', () => {
    expect(component.sucesso()).toBe(false);
  });

  it('isLoading começa false', () => {
    expect(component.isLoading()).toBe(false);
  });

  it('sem token na URL define erro no ngOnInit', () => {
    expect(component.token).toBeNull();
    expect(component.erro()).toBe('Link inválido. Solicite um novo.');
  });

  it('novaSenha inválida com menos de 6 caracteres', () => {
    component.form.get('novaSenha')!.setValue('123');
    expect(component.form.get('novaSenha')!.invalid).toBe(true);
  });

  it('novaSenha válida com 6+ caracteres', () => {
    component.form.get('novaSenha')!.setValue('senha123');
    component.form.get('confirmarSenha')!.setValue('senha123');
    expect(component.form.get('novaSenha')!.valid).toBe(true);
  });

  it('senhasIguais retorna null quando senhas são iguais', () => {
    const group = new FormGroup({
      novaSenha: new FormControl('igual123'),
      confirmarSenha: new FormControl('igual123')
    });
    expect(component.senhasIguais(group as any)).toBeNull();
  });

  it('senhasIguais retorna erro quando senhas diferem', () => {
    const group = new FormGroup({
      novaSenha: new FormControl('abc123'),
      confirmarSenha: new FormControl('xyz789')
    });
    expect(component.senhasIguais(group as any)).toEqual({ senhasDiferentes: true });
  });

  it('onSubmit não avança quando form inválido', () => {
    component.form.get('novaSenha')!.setValue('');
    component.onSubmit();
    expect(component.isLoading()).toBe(false);
  });

  it('form inválido quando senhas não coincidem', () => {
    component.form.setValue({ novaSenha: 'senha123', confirmarSenha: 'diferente' });
    expect(component.form.errors).toEqual({ senhasDiferentes: true });
  });
});
