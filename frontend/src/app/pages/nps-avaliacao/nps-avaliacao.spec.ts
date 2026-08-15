// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NpsAvaliacaoComponent } from './nps-avaliacao';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';

describe('NpsAvaliacaoComponent', () => {
  let component: NpsAvaliacaoComponent;
  let fixture: ComponentFixture<NpsAvaliacaoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NpsAvaliacaoComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(NpsAvaliacaoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('sem token na URL não faz chamada e encerra o carregamento', () => {
    expect(component.token).toBeNull();
    expect(component.isLoadingStatus()).toBe(false);
    expect(component.status()).toBeNull();
  });

  it('sucesso, isSubmitting e erro começam com valor neutro', () => {
    expect(component.sucesso()).toBe(false);
    expect(component.isSubmitting()).toBe(false);
    expect(component.erro()).toBe('');
  });

  it('notas vai de 0 a 10', () => {
    expect(component.notas).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  });

  it('escolherNota define a nota selecionada', () => {
    expect(component.scoreEscolhida()).toBeNull();
    component.escolherNota(9);
    expect(component.scoreEscolhida()).toBe(9);
  });

  it('onSubmit não avança sem nota selecionada', () => {
    component.onSubmit();
    expect(component.isSubmitting()).toBe(false);
  });

  it('onSubmit não avança sem token, mesmo com nota selecionada', () => {
    component.escolherNota(10);
    component.onSubmit();
    expect(component.isSubmitting()).toBe(false);
  });
});
