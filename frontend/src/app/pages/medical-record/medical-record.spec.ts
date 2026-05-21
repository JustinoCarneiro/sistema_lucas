// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MedicalRecordComponent } from './medical-record';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('MedicalRecordComponent', () => {
  let component: MedicalRecordComponent;
  let fixture: ComponentFixture<MedicalRecordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicalRecordComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(MedicalRecordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('nomePaciente começa com "Carregando..."', () => {
    expect(component.nomePaciente()).toBe('Carregando...');
  });

  it('historico começa como lista vazia', () => {
    expect(component.historico()).toEqual([]);
  });

  it('novasNotas começa como string vazia', () => {
    expect(component.novasNotas).toBe('');
  });

  it('isLoading começa true', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('historico pode ser preenchido manualmente', () => {
    const entradas = [
      { id: 1, nota: 'Primeira consulta', criadoEm: '2026-01-01' },
      { id: 2, nota: 'Retorno', criadoEm: '2026-02-01' }
    ];
    component.historico.set(entradas);
    expect(component.historico().length).toBe(2);
  });
});
