// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyProfileComponent } from './my-profile';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MfaService } from '../../security/mfa.service';
import { AuthService } from '../../security/auth.service';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { of, throwError } from 'rxjs';

vi.mock('qrcode', () => ({
  toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,fake')
}));

describe('MyProfileComponent', () => {
  let component: MyProfileComponent;
  let fixture: ComponentFixture<MyProfileComponent>;
  let mfaService: MfaService;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyProfileComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(MyProfileComponent);
    component = fixture.componentInstance;
    mfaService = TestBed.inject(MfaService);
    authService = TestBed.inject(AuthService);
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

  // --- MFA (SEC-02) ---

  it('iniciarSetupMfa popula mfaSetupData e renderiza o QR code', async () => {
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
    component.mfaSetupData.set({ secretBase32: 'S', otpAuthUri: 'x' });
    component.mfaQrCodeDataUrl.set('data:x');
    component.mfaEnableCode = '123456';

    component.cancelarSetupMfa();

    expect(component.mfaSetupData()).toBeNull();
    expect(component.mfaQrCodeDataUrl()).toBe('');
    expect(component.mfaEnableCode).toBe('');
  });

  it('confirmarAtivacaoMfa sem código define erro e não chama o serviço', () => {
    const enableSpy = vi.spyOn(mfaService, 'enable');
    component.mfaEnableCode = '';
    component.confirmarAtivacaoMfa();
    expect(enableSpy).not.toHaveBeenCalled();
  });

  it('confirmarAtivacaoMfa com sucesso mostra os backup codes e marca mfaEnabled', () => {
    component.profile.set({ mfaEnabled: false });
    vi.spyOn(mfaService, 'enable').mockReturnValue(of({ backupCodes: ['AAA', 'BBB'] }) as any);

    component.mfaEnableCode = '123456';
    component.confirmarAtivacaoMfa();

    expect(component.mfaBackupCodes()).toEqual(['AAA', 'BBB']);
    expect(component.profile().mfaEnabled).toBe(true);
    expect(component.mfaSetupData()).toBeNull();
  });

  it('confirmarAtivacaoMfa com código inválido não altera mfaEnabled', () => {
    component.profile.set({ mfaEnabled: false });
    vi.spyOn(mfaService, 'enable').mockReturnValue(throwError(() => 'Código inválido.'));

    component.mfaEnableCode = '000000';
    component.confirmarAtivacaoMfa();

    expect(component.profile().mfaEnabled).toBe(false);
    expect(component.mfaIsBusy()).toBe(false);
  });

  it('fecharBackupCodes limpa os códigos', () => {
    component.mfaBackupCodes.set(['AAA']);
    component.fecharBackupCodes();
    expect(component.mfaBackupCodes()).toBeNull();
  });

  it('openDisableMfaModal abre o modal e limpa campos', () => {
    component.disableMfaPassword = 'antiga';
    component.disableMfaCode = '111111';
    component.openDisableMfaModal();
    expect(component.showDisableMfaModal()).toBe(true);
    expect(component.disableMfaPassword).toBe('');
    expect(component.disableMfaCode).toBe('');
  });

  it('closeDisableMfaModal fecha o modal', () => {
    component.showDisableMfaModal.set(true);
    component.closeDisableMfaModal();
    expect(component.showDisableMfaModal()).toBe(false);
  });

  it('confirmarDesativacaoMfa sem senha/código define erro e não chama o serviço', () => {
    const disableSpy = vi.spyOn(mfaService, 'disable');
    component.disableMfaPassword = '';
    component.disableMfaCode = '';
    component.confirmarDesativacaoMfa();
    expect(disableSpy).not.toHaveBeenCalled();
  });

  it('confirmarDesativacaoMfa com sucesso desmarca mfaEnabled e desloga', () => {
    component.profile.set({ mfaEnabled: true });
    vi.spyOn(mfaService, 'disable').mockReturnValue(of({}) as any);
    const logoutSpy = vi.spyOn(authService, 'logout').mockImplementation(() => {});

    component.disableMfaPassword = 'senha123';
    component.disableMfaCode = '123456';
    component.confirmarDesativacaoMfa();

    expect(component.profile().mfaEnabled).toBe(false);
    expect(component.showDisableMfaModal()).toBe(false);
    expect(logoutSpy).toHaveBeenCalled();
  });
});
