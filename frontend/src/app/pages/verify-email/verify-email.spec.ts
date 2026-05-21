// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifyEmail } from './verify-email';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('VerifyEmail', () => {
  let component: VerifyEmail;
  let fixture: ComponentFixture<VerifyEmail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmail, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyEmail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('isSuccess começa false', () => {
    expect(component.isSuccess()).toBe(false);
  });

  it('sem token na URL — isError=true após ngOnInit', () => {
    expect(component.isError()).toBe(true);
  });

  it('sem token na URL — message indica token não encontrado', () => {
    expect(component.message()).toBe('Token de verificação não encontrado.');
  });

  it('message pode ser atualizada via signal', () => {
    component.message.set('Verificação concluída!');
    expect(component.message()).toBe('Verificação concluída!');
  });

  it('isSuccess e isError começam como estados mutuamente exclusivos', () => {
    expect(component.isSuccess() && component.isError()).toBe(false);
  });
});
