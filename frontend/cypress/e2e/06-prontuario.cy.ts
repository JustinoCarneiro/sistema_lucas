/// <reference types="cypress" />

describe('06 — Prontuários', () => {

  it('Profissional — agenda exibe consultas com ações', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    cy.contains('Minha Agenda', { timeout: 10000 }).should('be.visible');
    // A aba "Hoje" é a padrão e deve ter consultas do DataInitializer (c6, c7)
    cy.contains('Hoje').should('exist');
  });

  it('Profissional — dashboard exibe últimos prontuários', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/dashboard');
    cy.contains('Últimos prontuários', { timeout: 10000 }).should('be.visible');
    // Deve haver prontuários de Lucas criados no DataInitializer
    cy.contains('Lucas Silva').should('exist');
  });

});
