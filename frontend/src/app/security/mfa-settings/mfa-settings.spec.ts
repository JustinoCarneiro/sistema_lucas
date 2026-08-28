// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MfaSettingsComponent } from './mfa-settings';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MfaService } from '../mfa.service';
import { AuthService } from '../auth.service';
import { environment } from '../../../environments/environment';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { of, throwError } from 'rxjs';

vi.mock('qrcode', () => ({
  toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,fake')
}));

describe('MfaSettingsComponent', () => {
  let component: MfaSettingsComponent;
  let fixture: ComponentFixture<MfaSettingsComponent>;
  let httpMock: HttpTestingController;
  let mfaService: MfaService;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfaSettingsComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(MfaSettingsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    mfaService = TestBed.inject(MfaService);
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function respondeAuthMe(mfaEnabled: boolean) {
    const req = httpMock.expectOne(`${environment.apiUrl}/auth/me`);
    req.flush({ role: 'ADMIN', mfaEnabled });
  }

  it('should create', () => {
    respondeAuthMe(false);
    expect(component).toBeTruthy();
  });

  it('busca o próprio estado via GET /auth/me — funciona pra ADMIN, que não tem /me em Patient/Professional', () => {
    respondeAuthMe(true);
    expect(component.mfaEnabled()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  it('iniciarSetupMfa popula mfaSetupData e renderiza o QR code', async () => {
    respondeAuthMe(false);
    vi.spyOn(mfaService, 'setup').mockReturnValue(
      of({ secretBase32: 'SECRET', otpAuthUri: 'otpauth://totp/x' }) as any
    );

    component.iniciarSetupMfa();
    await new Promise(resolve => setTimeout(resolve, 0)); // aguarda o await interno do QRCode

    expect(component.mfaSetupData()).toEqual({ secretBase32: 'SECRET', otpAuthUri: 'otpauth://totp/x' });
    expect(component.mfaQrCodeDataUrl()).toBe('data:image/png;base64,fake');
    expect(component.mfaIsBusy()).toBe(false);
  });

  it('cancelarSetupMfa limpa o estado de setup', () => {
    respondeAuthMe(false);
    component.mfaSetupData.set({ secretBase32: 'S', otpAuthUri: 'x' });
    component.mfaQrCodeDataUrl.set('data:x');
    component.mfaEnableCode = '123456';

    component.cancelarSetupMfa();

    expect(component.mfaSetupData()).toBeNull();
    expect(component.mfaQrCodeDataUrl()).toBe('');
    expect(component.mfaEnableCode).toBe('');
  });

  it('confirmarAtivacaoMfa sem código define erro e não chama o serviço', () => {
    respondeAuthMe(false);
    const enableSpy = vi.spyOn(mfaService, 'enable');
    component.mfaEnableCode = '';
    component.confirmarAtivacaoMfa();
    expect(enableSpy).not.toHaveBeenCalled();
  });

  it('confirmarAtivacaoMfa com sucesso mostra os backup codes e marca mfaEnabled', () => {
    respondeAuthMe(false);
    vi.spyOn(mfaService, 'enable').mockReturnValue(of({ backupCodes: ['AAA', 'BBB'] }) as any);

    component.mfaEnableCode = '123456';
    component.confirmarAtivacaoMfa();

    expect(component.mfaBackupCodes()).toEqual(['AAA', 'BBB']);
    expect(component.mfaEnabled()).toBe(true);
    expect(component.mfaSetupData()).toBeNull();
  });

  it('confirmarAtivacaoMfa com código inválido não altera mfaEnabled', () => {
    respondeAuthMe(false);
    vi.spyOn(mfaService, 'enable').mockReturnValue(throwError(() => 'Código inválido.'));

    component.mfaEnableCode = '000000';
    component.confirmarAtivacaoMfa();

    expect(component.mfaEnabled()).toBe(false);
    expect(component.mfaIsBusy()).toBe(false);
  });

  it('fecharBackupCodes limpa os códigos', () => {
    respondeAuthMe(false);
    component.mfaBackupCodes.set(['AAA']);
    component.fecharBackupCodes();
    expect(component.mfaBackupCodes()).toBeNull();
  });

  it('openDisableMfaModal abre o modal e limpa campos', () => {
    respondeAuthMe(true);
    component.disableMfaPassword = 'antiga';
    component.disableMfaCode = '111111';
    component.openDisableMfaModal();
    expect(component.showDisableMfaModal()).toBe(true);
    expect(component.disableMfaPassword).toBe('');
    expect(component.disableMfaCode).toBe('');
  });

  it('closeDisableMfaModal fecha o modal', () => {
    respondeAuthMe(true);
    component.showDisableMfaModal.set(true);
    component.closeDisableMfaModal();
    expect(component.showDisableMfaModal()).toBe(false);
  });

  it('confirmarDesativacaoMfa sem senha/código define erro e não chama o serviço', () => {
    respondeAuthMe(true);
    const disableSpy = vi.spyOn(mfaService, 'disable');
    component.disableMfaPassword = '';
    component.disableMfaCode = '';
    component.confirmarDesativacaoMfa();
    expect(disableSpy).not.toHaveBeenCalled();
  });

  it('confirmarDesativacaoMfa com sucesso desmarca mfaEnabled e desloga', () => {
    respondeAuthMe(true);
    vi.spyOn(mfaService, 'disable').mockReturnValue(of({}) as any);
    const logoutSpy = vi.spyOn(authService, 'logout').mockImplementation(() => {});

    component.disableMfaPassword = 'senha123';
    component.disableMfaCode = '123456';
    component.confirmarDesativacaoMfa();

    expect(component.mfaEnabled()).toBe(false);
    expect(component.showDisableMfaModal()).toBe(false);
    expect(logoutSpy).toHaveBeenCalled();
  });
});
