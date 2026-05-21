// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Appointments } from './appointments';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('Appointments', () => {
  let component: Appointments;
  let fixture: ComponentFixture<Appointments>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Appointments, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(Appointments);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('isLoading começa true enquanto aguarda resposta do servidor', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('labelStatus retorna rótulo em português para todos os status conhecidos', () => {
    expect(component.labelStatus('AGENDADA')).toBe('Agendada');
    expect(component.labelStatus('CONFIRMADA_PROFISSIONAL')).toBe('Aguardando paciente');
    expect(component.labelStatus('CONFIRMADA')).toBe('Confirmada');
    expect(component.labelStatus('CONCLUIDA')).toBe('Concluída');
    expect(component.labelStatus('CANCELADA')).toBe('Cancelada');
    expect(component.labelStatus('FALTA')).toBe('Faltou');
  });

  it('labelStatus retorna o próprio valor para status desconhecido', () => {
    expect(component.labelStatus('STATUS_INVALIDO')).toBe('STATUS_INVALIDO');
    expect(component.labelStatus('OUTRO')).toBe('OUTRO');
  });

  it('consultas começa como lista vazia', () => {
    expect(component.consultas()).toEqual([]);
  });

  it('openDetails define selectedItem', () => {
    const consulta = { id: 42, status: 'AGENDADA' };
    component.openDetails(consulta);
    expect(component.selectedItem).toBe(consulta);
  });

  it('closeDetails limpa selectedItem', () => {
    component.selectedItem = { id: 1 };
    component.closeDetails();
    expect(component.selectedItem).toBeNull();
  });
});
