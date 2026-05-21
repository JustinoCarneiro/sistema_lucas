// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyAvailabilityComponent } from './my-availability';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, it, expect } from 'vitest';

describe('MyAvailabilityComponent', () => {
  let component: MyAvailabilityComponent;
  let fixture: ComponentFixture<MyAvailabilityComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyAvailabilityComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(MyAvailabilityComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('viewedMonthOffset começa em 1 (próximo mês por padrão)', () => {
    expect(component.viewedMonthOffset()).toBe(1);
  });

  it('bloqueado começa false', () => {
    expect(component.bloqueado()).toBe(false);
  });

  it('isSaving começa false', () => {
    expect(component.isSaving()).toBe(false);
  });

  it('successMessage e errorMessage começam vazios', () => {
    expect(component.successMessage()).toBe('');
    expect(component.errorMessage()).toBe('');
  });

  it('weekDays contém os 7 dias abreviados', () => {
    expect(component.weekDays.length).toBe(7);
    expect(component.weekDays[0]).toBe('Dom');
    expect(component.weekDays[6]).toBe('Sáb');
  });

  it('possibleSlots contém 11 horários de 08:00 a 18:00', () => {
    expect(component.possibleSlots.length).toBe(11);
    expect(component.possibleSlots[0]).toBe('08:00');
    expect(component.possibleSlots[10]).toBe('18:00');
  });

  it('hasSubmitted é false quando não há slots selecionados', () => {
    component.calendarDays.set([
      { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: [] }
    ]);
    expect(component.hasSubmitted()).toBe(false);
  });

  it('hasSubmitted é true quando algum dia tem slots', () => {
    component.calendarDays.set([
      { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: ['09:00'] }
    ]);
    expect(component.hasSubmitted()).toBe(true);
  });

  it('showAlert é false quando diasRestantes é null', () => {
    component.diasRestantes.set(null);
    expect(component.showAlert()).toBe(false);
  });

  it('showAlert é true quando bloqueado=true', () => {
    component.diasRestantes.set(20);
    component.bloqueado.set(true);
    expect(component.showAlert()).toBe(true);
  });

  it('showAlert é true quando diasRestantes < 15 e não enviou', () => {
    component.diasRestantes.set(10);
    component.bloqueado.set(false);
    component.calendarDays.set([]);
    expect(component.showAlert()).toBe(true);
  });

  it('showAlert é false quando hasSubmitted=true mesmo com poucos dias', () => {
    component.diasRestantes.set(5);
    component.bloqueado.set(false);
    component.calendarDays.set([
      { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: ['09:00'] }
    ]);
    expect(component.showAlert()).toBe(false);
  });

  it('isSlotSelected retorna false quando não há dia selecionado', () => {
    component.selectedDate.set(null);
    expect(component.isSlotSelected('09:00')).toBe(false);
  });

  it('isSlotSelected retorna true para slot selecionado no dia ativo', () => {
    const day = { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: ['09:00', '10:00'] };
    component.selectedDate.set(day);
    expect(component.isSlotSelected('09:00')).toBe(true);
    expect(component.isSlotSelected('11:00')).toBe(false);
  });

  it('toggleSlot adiciona slot quando não selecionado', () => {
    const day = { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: [] };
    component.calendarDays.set([day]);
    component.selectedDate.set(day);
    component.toggleSlot('09:00');
    expect(component.selectedDate()?.selectedSlots).toContain('09:00');
  });

  it('toggleSlot remove slot quando já selecionado', () => {
    const day = { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: ['09:00'] };
    component.calendarDays.set([day]);
    component.selectedDate.set(day);
    component.toggleSlot('09:00');
    expect(component.selectedDate()?.selectedSlots).not.toContain('09:00');
  });

  it('toggleSlot não modifica slots quando bloqueado=true', () => {
    component.bloqueado.set(true);
    const day = { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: [] };
    component.selectedDate.set(day);
    component.toggleSlot('09:00');
    expect(component.selectedDate()?.selectedSlots.length).toBe(0);
  });

  it('selectDate não seleciona dia fora do mês atual', () => {
    const outsideDay = { dateStr: '', dayNum: 0, isCurrentMonth: false, selectedSlots: [] };
    component.selectDate(outsideDay);
    expect(component.selectedDate()).toBeNull();
  });

  it('selectDate define o dia quando isCurrentMonth=true', () => {
    const day = { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: [] };
    component.selectDate(day);
    expect(component.selectedDate()?.dateStr).toBe('2026-06-15');
  });

  it('enabledDaysCount conta dias com pelo menos 1 slot', () => {
    component.calendarDays.set([
      { dateStr: '2026-06-15', dayNum: 15, isCurrentMonth: true, selectedSlots: ['09:00'] },
      { dateStr: '2026-06-16', dayNum: 16, isCurrentMonth: true, selectedSlots: [] },
      { dateStr: '2026-06-17', dayNum: 17, isCurrentMonth: true, selectedSlots: ['10:00', '11:00'] }
    ]);
    expect(component.enabledDaysCount).toBe(2);
  });
});
