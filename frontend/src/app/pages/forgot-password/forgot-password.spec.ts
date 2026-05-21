// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form inicialmente inválido com email vazio', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('form válido com email correto', () => {
    component.form.get('email')!.setValue('user@test.com');
    expect(component.form.valid).toBe(true);
  });

  it('form inválido com email em formato incorreto', () => {
    component.form.get('email')!.setValue('nao-e-email');
    expect(component.form.get('email')!.invalid).toBe(true);
  });

  it('enviado começa false', () => {
    expect(component.enviado()).toBe(false);
  });

  it('isLoading começa false', () => {
    expect(component.isLoading()).toBe(false);
  });

  it('onSubmit com form inválido não dispara request (isLoading permanece false)', () => {
    component.form.get('email')!.setValue('');
    component.onSubmit();
    expect(component.isLoading()).toBe(false);
    expect(component.enviado()).toBe(false);
  });

  it('onSubmit com form válido define isLoading=true', () => {
    component.form.get('email')!.setValue('user@test.com');
    component.onSubmit();
    expect(component.isLoading()).toBe(true);
  });
});
