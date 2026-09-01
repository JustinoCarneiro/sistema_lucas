// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyAppointmentsComponent } from './my-appointments';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { NotificationService } from '../../notification.service';

describe('MyAppointmentsComponent (Carregamento de Datas Disponíveis)', () => {
  let component: MyAppointmentsComponent;
  let fixture: ComponentFixture<MyAppointmentsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MyAppointmentsComponent,
        HttpClientTestingModule,
        ReactiveFormsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyAppointmentsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('onProfessionalChange sem profissional selecionado não busca datas nem quebra o estado', () => {
    component.scheduleForm.patchValue({ professionalId: '' });

    component.onProfessionalChange();

    expect(component.selectedProfessional()).toBeNull();
    expect(component.availableDates()).toEqual([]);
    expect(component.availableSlots()).toEqual([]);
    // Nenhuma requisição deve ter sido disparada — httpMock.verify() no afterEach garante isso.
  });

  it('onProfessionalChange busca as datas disponíveis do profissional selecionado e formata pra exibição', () => {
    component.professionals.set([{ id: 1, name: 'Dra. Ana' }]);
    component.scheduleForm.patchValue({ professionalId: '1' });

    component.onProfessionalChange();

    expect(component.selectedProfessional()?.name).toBe('Dra. Ana');

    const req = httpMock.expectOne(`${environment.apiUrl}/disponibilidade/1/available-dates`);
    expect(req.request.method).toBe('GET');
    req.flush(['2026-04-15', '2026-04-20']); // quarta-feira, segunda-feira seguinte

    const dates = component.availableDates();
    expect(dates.length).toBe(2);
    expect(dates[0].value).toBe('2026-04-15');
    expect(dates[0].label).toContain('quarta-feira');
    expect(dates[1].value).toBe('2026-04-20');
    expect(dates[1].label).toContain('segunda-feira');
  });

  it('bloqueio por penalidade vira aviso ("Atenção"), não o vermelho "Erro Detectado"', () => {
    const notify = TestBed.inject(NotificationService);
    const warningSpy = vi.spyOn(notify, 'warning');
    const errorSpy = vi.spyOn(notify, 'error');

    (component as any).notifyScheduleError(
      'Você está temporariamente bloqueado para novos agendamentos até 15/09/2026 12:07 devido a um cancelamento/reagendamento tardio.'
    );

    expect(warningSpy).toHaveBeenCalledTimes(1);
    expect(errorSpy).not.toHaveBeenCalled();
  });

  it('erro comum de agendamento continua indo como erro', () => {
    const notify = TestBed.inject(NotificationService);
    const warningSpy = vi.spyOn(notify, 'warning');
    const errorSpy = vi.spyOn(notify, 'error');

    (component as any).notifyScheduleError('Este horário já está ocupado. Escolha outro horário.');

    expect(errorSpy).toHaveBeenCalledTimes(1);
    expect(warningSpy).not.toHaveBeenCalled();
  });

  it('onProfessionalChange reseta seleção de data/slot ao trocar de profissional', () => {
    component.professionals.set([{ id: 1, name: 'Dra. Ana' }]);
    component.scheduleForm.patchValue({ professionalId: '1' });
    component.selectedDate.set('2026-04-15');
    component.selectedSlot.set({ startTime: '09:00' });
    component.availableSlots.set([{ startTime: '09:00' }]);

    component.onProfessionalChange();
    httpMock.expectOne(`${environment.apiUrl}/disponibilidade/1/available-dates`).flush([]);

    expect(component.selectedDate()).toBe('');
    expect(component.selectedSlot()).toBeNull();
    expect(component.availableSlots()).toEqual([]);
  });
});
