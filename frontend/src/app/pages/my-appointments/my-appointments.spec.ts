// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyAppointmentsComponent } from './my-appointments';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { vi, describe, beforeEach, it, expect } from 'vitest';

describe('MyAppointmentsComponent (Lógica de Datas)', () => {
  let component: MyAppointmentsComponent;
  let fixture: ComponentFixture<MyAppointmentsComponent>;

  beforeEach(async () => {
    // Nota: O ambiente de teste deve ser inicializado pelo runner do projeto (ng test).
    // Este teste assume que o TestBed está pronto para configuração.
    await TestBed.configureTestingModule({
      imports: [
        MyAppointmentsComponent,
        HttpClientTestingModule,
        ReactiveFormsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyAppointmentsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deve gerar apenas a próxima ocorrência para cada dia da semana', () => {
    const days = ['MONDAY', 'WEDNESDAY'];
    
    // Usando Vitest para mockar a data (substituindo jasmine.clock)
    vi.useFakeTimers();
    const mockDate = new Date(2026, 3, 14); // 14 de Abril de 2026 (Terça)
    vi.setSystemTime(mockDate);

    component.generateAvailableDates(days);
    const dates = component.availableDates();

    expect(dates.length).toBe(2);
    expect(dates[0].value).toBe('2026-04-15'); // Quarta
    expect(dates[0].label).toContain('quarta-feira');
    expect(dates[1].value).toBe('2026-04-20'); // Segunda (próxima semana)
    expect(dates[1].label).toContain('segunda-feira');

    vi.useRealTimers();
  });

  it('deve incluir o próprio dia se o profissional atende hoje', () => {
    const days = ['WEDNESDAY'];
    
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 3, 15)); // Hoje é Quarta

    component.generateAvailableDates(days);
    const dates = component.availableDates();

    expect(dates.length).toBe(1);
    expect(dates[0].value).toBe('2026-04-15');
    expect(dates[0].label).toContain('quarta-feira');

    vi.useRealTimers();
  });
});
