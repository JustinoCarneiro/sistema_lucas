// @vitest-environment jsdom
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfessionalsComponent } from './professionals';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

describe('ProfessionalsComponent', () => {
  let component: ProfessionalsComponent;
  let fixture: ComponentFixture<ProfessionalsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessionalsComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfessionalsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('mostrarFormulario inicia como false', () => {
    expect(component.mostrarFormulario()).toBe(false);
  });

  it('isEditing inicia como false', () => {
    expect(component.isEditing()).toBe(false);
  });

  it('form inicialmente inválido com campos obrigatórios vazios', () => {
    expect(component.professionalForm.invalid).toBe(true);
  });

  it('form válido com todos os campos obrigatórios preenchidos', () => {
    component.professionalForm.setValue({
      name: 'Dr. Teste', email: 'dr@test.com', password: 'senha123',
      tipoRegistro: 'CRP', registroConselho: '12345-SP', specialty: 'Psicologia'
    });
    expect(component.professionalForm.valid).toBe(true);
  });

  it('campo email inválido com formato incorreto', () => {
    component.professionalForm.get('email')!.setValue('invalido');
    expect(component.professionalForm.get('email')!.invalid).toBe(true);
  });

  it('editProfessional preenche o form e ativa modo edição', () => {
    const prof = {
      id: 1, name: 'Dr. X', email: 'x@test.com',
      tipoRegistro: 'CRM', registroConselho: '99999-SP', specialty: 'Psiquiatria'
    };
    component.editProfessional(prof);
    expect(component.isEditing()).toBe(true);
    expect(component.mostrarFormulario()).toBe(true);
    expect(component.currentProfessionalId()).toBe(1);
    expect(component.professionalForm.get('name')!.value).toBe('Dr. X');
  });

  it('cancelEdit reseta o form e sai do modo edição', () => {
    component.mostrarFormulario.set(true);
    component.isEditing.set(true);
    component.currentProfessionalId.set(42);
    component.cancelEdit();
    expect(component.mostrarFormulario()).toBe(false);
    expect(component.isEditing()).toBe(false);
    expect(component.currentProfessionalId()).toBeNull();
  });

  it('openDetails define selectedItem', () => {
    const item = { id: 1, name: 'Dr. Teste' };
    component.openDetails(item);
    expect(component.selectedItem).toBe(item);
  });

  it('closeDetails limpa selectedItem', () => {
    component.selectedItem = { id: 1 };
    component.closeDetails();
    expect(component.selectedItem).toBeNull();
  });
});
