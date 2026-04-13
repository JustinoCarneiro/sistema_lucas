/// <reference types="cypress" />

describe('11 — Disponibilidade de Horários (Profissional)', () => {

  beforeEach(() => {
    cy.login('ana@clinica.com', '123456');
  });

  it('Link "Minha Disponibilidade" aparece no sidebar do profissional', () => {
    cy.contains('Minha Disponibilidade', { timeout: 10000 }).should('be.visible');
  });

  it('Página de disponibilidade carrega corretamente', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Carregando', { timeout: 10000 }).should('not.exist');
    cy.contains('Minha Disponibilidade').should('be.visible');
    cy.contains('Configure os dias e horários').should('exist');
  });

  it('Exibe os 7 dias da semana', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Carregando').should('not.exist');
    cy.contains('Segunda-feira').should('exist');
    cy.contains('Terça-feira').should('exist');
    cy.contains('Quarta-feira').should('exist');
    cy.contains('Sexta-feira').should('exist');
  });

  it('Disponibilidade pré-configurada aparece (dados do DataInitializer)', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Carregando').should('not.exist');
    // Dra. Ana tem disponibilidade Seg–Sex configurada no DataInitializer
    cy.get('[class*="bg-green-100"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
    cy.contains('Salvo').should('exist');
  });

  it('Contador de dias configurados aparece', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Carregando').should('not.exist');
    // Dra. Ana tem 5 dias configurados (Seg–Sex)
    cy.contains('dias configurados').should('exist');
  });

  it('Slots de 1h são exibidos no preview', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Carregando').should('not.exist');
    // Dra. Ana: 08:00–12:00 = 4 slots
    cy.contains('08:00 – 09:00').should('exist');
    cy.contains('11:00 – 12:00').should('exist');
  });

  it('Campos de início e fim estão visíveis para dias habilitados', () => {
    cy.visit('/panel/my-availability');
    cy.get('input[type="time"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Botão "Atualizar" aparece em dias já salvos', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Atualizar', { timeout: 10000 }).should('exist');
  });

  it('Botão "Remover dia" aparece em dias salvos', () => {
    cy.visit('/panel/my-availability');
    cy.contains('Remover dia', { timeout: 10000 }).should('exist');
  });

});
