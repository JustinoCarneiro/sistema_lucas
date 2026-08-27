// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MfaVerifyComponent } from './mfa-verify';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { MfaService } from '../../security/mfa.service';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { of, throwError } from 'rxjs';

describe('MfaVerifyComponent', () => {
  let component: MfaVerifyComponent;
  let fixture: ComponentFixture<MfaVerifyComponent>;
  let mfaService: MfaService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfaVerifyComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(MfaVerifyComponent);
    component = fixture.componentInstance;
    mfaService = TestBed.inject(MfaService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('começa sem código de backup e sem erro', () => {
    expect(component.usarBackupCode()).toBe(false);
    expect(component.errorMessage()).toBe('');
  });

  it('toggleBackupCode alterna o modo e limpa código/erro', () => {
    component.code = '123456';
    component.errorMessage.set('erro anterior');
    component.toggleBackupCode();
    expect(component.usarBackupCode()).toBe(true);
    expect(component.code).toBe('');
    expect(component.errorMessage()).toBe('');
  });

  it('onSubmit sem código define erro e não chama o serviço', () => {
    const verifySpy = vi.spyOn(mfaService, 'verify');
    component.code = '';
    component.onSubmit();
    expect(component.errorMessage()).toBe('Informe o código.');
    expect(verifySpy).not.toHaveBeenCalled();
  });

  it('onSubmit com sucesso grava role/verified e navega pro painel', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(mfaService, 'verify').mockReturnValue(of({ role: 'PATIENT', verified: true }) as any);

    component.code = '123456';
    component.onSubmit();

    expect(localStorage.getItem('role')).toBe('PATIENT');
    expect(navigateSpy).toHaveBeenCalledWith(['/panel']);
    expect(component.isLoading()).toBe(false);
  });

  it('onSubmit com código inválido define errorMessage e isLoading=false', () => {
    vi.spyOn(mfaService, 'verify').mockReturnValue(throwError(() => 'Código inválido.'));

    component.code = '000000';
    component.onSubmit();

    expect(component.errorMessage()).toBe('Código inválido.');
    expect(component.isLoading()).toBe(false);
  });
});
