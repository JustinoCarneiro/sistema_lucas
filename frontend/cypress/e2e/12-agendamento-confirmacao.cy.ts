/// <reference types="cypress" />

describe('12 — Agendamento Guiado (Paciente → Slots)', () => {

  beforeEach(() => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    cy.contains('Carregando', { timeout: 10000 }).should('not.exist');
  });

  it('Formulário de agendamento exibe select de profissionais', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.contains('Nova consulta').should('be.visible');
    cy.get('select').first().should('exist');
  });

  it('Campo de data (select) aparece após selecionar profissional', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.get('select').first().select(1); // Ana
    cy.contains('Escolha a data', { timeout: 5000 }).should('be.visible');
    cy.get('select#date').should('exist');
  });

  it('Slots aparecem após selecionar uma data válida', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.get('select').first().select(1); 
    
    // Seleciona a SEGUNDA data disponível no select (garante estar no futuro)
    cy.get('select#date').should('exist');
    cy.get('select#date option').should('have.length.at.least', 3); // Placeholder + Today + Future
    cy.get('select#date').select(2);
    
    // Deve buscar horários
    cy.contains('Escolha o horário disponível', { timeout: 10000 }).should('be.visible');
    
    // Verifica se existem botões de horários
    cy.get('button').filter(':contains(":00")').should('have.length.at.least', 1);
  });

  it('Fluxo completo: Agendar consulta em um slot livre', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.get('select').first().select(1);
    
    // Seleciona uma data futura
    cy.get('select#date').select(2);
    
    // Clica no primeiro slot de 1h disponível usando o novo testid
    cy.contains('Buscando horários').should('not.exist');
    cy.get('[data-testid="slot-button"]').first().should('be.visible').click({ force: true });
    
    // Agora o campo de motivo DEVE aparecer (usando o novo testid)
    cy.get('[data-testid="reason-input"]', { timeout: 20000 })
      .should('be.visible')
      .type('Teste Cypress — Fluxo Completo');
    
    // Garante que o botão de confirmar está visível e clica
    cy.contains('button', 'Confirmar agendamento').scrollIntoView().click({ force: true });
    
    // Sucesso — Volta para a listagem e vê o novo agendamento
    cy.contains('Agendada', { timeout: 30000 }).should('be.visible');
    cy.contains('Teste Cypress — Fluxo Completo').scrollIntoView().should('be.visible');
  });

});
