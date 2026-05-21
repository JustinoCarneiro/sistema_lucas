// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Patients } from './patients';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('Patients', () => {
  let component: Patients;
  let fixture: ComponentFixture<Patients>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Patients, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(Patients);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('filtroStatus inicia como TODOS', () => {
    expect(component.filtroStatus()).toBe('TODOS');
  });

  it('filteredPatients TODOS retorna todos sem filtro', () => {
    component.patientsList.set([
      { id: 1, blockedUntil: null, infractionCount: 0 },
      { id: 2, blockedUntil: '2099-01-01', infractionCount: 1 }
    ]);
    component.filtroStatus.set('TODOS');
    expect(component.filteredPatients().length).toBe(2);
  });

  it('filteredPatients BLOQUEADOS retorna apenas pacientes com blockedUntil no futuro', () => {
    component.patientsList.set([
      { id: 1, blockedUntil: null, infractionCount: 0 },
      { id: 2, blockedUntil: '2099-01-01', infractionCount: 1 },
      { id: 3, blockedUntil: '2000-01-01', infractionCount: 1 }
    ]);
    component.filtroStatus.set('BLOQUEADOS');
    const resultado = component.filteredPatients();
    expect(resultado.length).toBe(1);
    expect(resultado[0].id).toBe(2);
  });

  it('filteredPatients COM_INFRACOES retorna apenas pacientes com infractionCount > 0', () => {
    component.patientsList.set([
      { id: 1, blockedUntil: null, infractionCount: 0 },
      { id: 2, blockedUntil: null, infractionCount: 2 },
      { id: 3, blockedUntil: null, infractionCount: 1 }
    ]);
    component.filtroStatus.set('COM_INFRACOES');
    expect(component.filteredPatients().length).toBe(2);
  });

  it('filteredPatients BLOQUEADOS exclui pacientes com blockedUntil expirado', () => {
    component.patientsList.set([
      { id: 1, blockedUntil: '2000-01-01', infractionCount: 1 }
    ]);
    component.filtroStatus.set('BLOQUEADOS');
    expect(component.filteredPatients().length).toBe(0);
  });

  it('openDetails define selectedItem', () => {
    const item = { id: 1, name: 'Paciente Teste' };
    component.openDetails(item);
    expect(component.selectedItem).toBe(item);
  });

  it('closeDetails limpa selectedItem', () => {
    component.selectedItem = { id: 1 };
    component.closeDetails();
    expect(component.selectedItem).toBeNull();
  });
});
