// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MyDocumentsComponent } from './my-documents';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, it, expect } from 'vitest';

describe('MyDocumentsComponent', () => {
  let component: MyDocumentsComponent;
  let fixture: ComponentFixture<MyDocumentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyDocumentsComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(MyDocumentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('documentos começa como lista vazia', () => {
    expect(component.documentos()).toEqual([]);
  });

  it('isLoading começa true', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('tiposLabel contém todos os tipos de documento', () => {
    expect(component.tiposLabel['LAUDO_PSICOLOGICO']).toBe('Laudo Psicológico');
    expect(component.tiposLabel['RELATORIO_EVOLUCAO']).toBe('Relatório de Evolução');
    expect(component.tiposLabel['ENCAMINHAMENTO']).toBe('Encaminhamento');
    expect(component.tiposLabel['ATESTADO']).toBe('Atestado');
    expect(component.tiposLabel['AVALIACAO_PSICOLOGICA']).toBe('Avaliação Psicológica');
    expect(component.tiposLabel['RECEITA_PRESCRICAO']).toBe('Receita / Prescrição');
  });

  it('baixarPdf com arquivoBase64 null não lança erro', () => {
    expect(() => component.baixarPdf({ arquivoBase64: null })).not.toThrow();
  });

  it('baixarPdf com arquivoBase64 undefined não lança erro', () => {
    expect(() => component.baixarPdf({})).not.toThrow();
  });
});
