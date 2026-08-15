// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WaitlistComponent } from './waitlist';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('WaitlistComponent', () => {
  let component: WaitlistComponent;
  let fixture: ComponentFixture<WaitlistComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WaitlistComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(WaitlistComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form começa inválido', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('form fica válido com profissional, data e horário preenchidos', () => {
    component.form.setValue({ professionalId: '1', date: '2026-12-01', time: '15:00' });
    expect(component.form.valid).toBe(true);
  });

  it('isSubmitting e isLoading começam com valor neutro', () => {
    expect(component.isSubmitting()).toBe(false);
  });

  it('onSubmit não avança com form inválido', () => {
    component.onSubmit();
    expect(component.isSubmitting()).toBe(false);
  });
});
