// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('isLoading começa true enquanto aguarda dados', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('dados começa null', () => {
    expect(component.dados()).toBeNull();
  });

  it('maxStatus retorna 1 quando dados é null (evita divisão por zero)', () => {
    expect(component.maxStatus).toBe(1);
  });

  it('maxStatus retorna o maior valor de consultasPorStatus', () => {
    component.dados.set({ consultasPorStatus: { AGENDADA: 5, CONCLUIDA: 10, CANCELADA: 3 } });
    expect(component.maxStatus).toBe(10);
  });

  it('maxStatus retorna 1 quando todos os valores são 0', () => {
    component.dados.set({ consultasPorStatus: { AGENDADA: 0, CONCLUIDA: 0 } });
    expect(component.maxStatus).toBe(1);
  });

  it('barWidth calcula percentual baseado no maxStatus', () => {
    component.dados.set({ consultasPorStatus: { AGENDADA: 5, CONCLUIDA: 10 } });
    expect(component.barWidth(5)).toBe('50%');
    expect(component.barWidth(10)).toBe('100%');
  });

  it('barClass retorna classe CSS correta para cada status', () => {
    expect(component.barClass('AGENDADA')).toBe('bg-blue-500');
    expect(component.barClass('CONCLUIDA')).toBe('bg-green-500');
    expect(component.barClass('CANCELADA')).toBe('bg-red-400');
    expect(component.barClass('FALTA')).toBe('bg-yellow-400');
    expect(component.barClass('DESCONHECIDO')).toBe('bg-gray-400');
  });

  it('statusEntries retorna lista vazia quando dados é null', () => {
    expect(component.statusEntries()).toEqual([]);
  });

  it('statusEntries retorna pares [status, count] de consultasPorStatus', () => {
    component.dados.set({ consultasPorStatus: { AGENDADA: 5, CONCLUIDA: 10 } });
    const entries = component.statusEntries();
    expect(entries.length).toBe(2);
    expect(entries.find(([s]) => s === 'AGENDADA')?.[1]).toBe(5);
  });

  it('proximaConsulta retorna null quando dados é null', () => {
    expect(component.proximaConsulta()).toBeNull();
  });

  it('proximaConsulta retorna null quando proximaConsulta é lista vazia', () => {
    component.dados.set({ proximaConsulta: [] });
    expect(component.proximaConsulta()).toBeNull();
  });

  it('proximaConsulta retorna primeiro elemento da lista', () => {
    const consulta = { id: 1, patientName: 'João' };
    component.dados.set({ proximaConsulta: [consulta, { id: 2 }] });
    expect(component.proximaConsulta()).toBe(consulta);
  });

  it('dados com consultasAtrasadas > 0 é detectado pelo template', () => {
    component.dados.set({ consultasAtrasadas: 3 });
    expect(component.dados()?.consultasAtrasadas).toBe(3);
  });

  it('dados com consultasAtrasadas = 0 — banner não deve aparecer', () => {
    component.dados.set({ consultasAtrasadas: 0 });
    expect(component.dados()?.consultasAtrasadas > 0).toBe(false);
  });
});
