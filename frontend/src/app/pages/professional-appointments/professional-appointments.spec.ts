// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfessionalAppointmentsComponent } from './professional-appointments';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('ProfessionalAppointmentsComponent', () => {
  let component: ProfessionalAppointmentsComponent;
  let fixture: ComponentFixture<ProfessionalAppointmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessionalAppointmentsComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfessionalAppointmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('abaAtiva começa como hoje', () => {
    expect(component.abaAtiva()).toBe('hoje');
  });

  it('consultasAtrasadas começa como lista vazia', () => {
    expect(component.consultasAtrasadas()).toEqual([]);
  });

  it('isLoadingAtrasadas fica true após ngOnInit (carregarAtrasadas chamado)', () => {
    expect(component.isLoadingAtrasadas()).toBe(true);
  });

  it('statusLabel contém todos os status do sistema de consultas', () => {
    expect(component.statusLabel['AGENDADA']).toBe('Agendada');
    expect(component.statusLabel['CONFIRMADA']).toBe('Confirmada');
    expect(component.statusLabel['CANCELADA']).toBe('Cancelada');
    expect(component.statusLabel['FALTA']).toBe('Faltou');
    expect(component.statusLabel['AGUARDANDO_CONFIRMACAO']).toBe('Aguardando Confirmação');
    expect(component.statusLabel['CONCLUIDA']).toBe('Concluída');
  });

  it('abaAtiva pode ser alterada para atrasadas', () => {
    component.abaAtiva.set('atrasadas');
    expect(component.abaAtiva()).toBe('atrasadas');
  });

  it('consultasAtrasadas sinal reflete lista injetada manualmente', () => {
    const atrasadas = [
      { id: 1, patientName: 'João', status: 'AGENDADA' },
      { id: 2, patientName: 'Maria', status: 'CONFIRMADA' }
    ];
    component.consultasAtrasadas.set(atrasadas);
    expect(component.consultasAtrasadas().length).toBe(2);
    expect(component.consultasAtrasadas()[0].id).toBe(1);
  });

  it('consultasHoje e proximasConsultas começam como listas vazias', () => {
    expect(component.consultasHoje()).toEqual([]);
    expect(component.proximasConsultas()).toEqual([]);
  });

  it('today é instância de Date', () => {
    expect(component.today).toBeInstanceOf(Date);
  });
});
