// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DocumentManagementComponent } from './document-management';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, it, expect } from 'vitest';

describe('DocumentManagementComponent', () => {
  let component: DocumentManagementComponent;
  let fixture: ComponentFixture<DocumentManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentManagementComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('documentos começa como lista vazia', () => {
    expect(component.documentos()).toEqual([]);
  });

  it('pacientes começa como lista vazia', () => {
    expect(component.pacientes()).toEqual([]);
  });

  it('isLoading começa true', () => {
    expect(component.isLoading()).toBe(true);
  });

  it('mostrarFormulario começa false', () => {
    expect(component.mostrarFormulario()).toBe(false);
  });

  it('openDetails define selectedItem', () => {
    const item = { id: 1, tipo: 'ATESTADO' };
    component.openDetails(item);
    expect(component.selectedItem).toBe(item);
  });

  it('closeDetails limpa selectedItem', () => {
    component.selectedItem = { id: 1 };
    component.closeDetails();
    expect(component.selectedItem).toBeNull();
  });

  it('abrirPdf com base64 vazio não lança erro', () => {
    expect(() => component.abrirPdf('', 'arquivo.pdf')).not.toThrow();
  });
});
