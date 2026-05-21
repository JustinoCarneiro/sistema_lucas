/// <reference types="cypress" />

describe('16 — Política de Privacidade', () => {

  beforeEach(() => {
    cy.visit('/privacidade');
  });

  it('página carrega sem necessidade de autenticação', () => {
    cy.url().should('include', '/privacidade');
  });

  it('contém seções principais da política de privacidade', () => {
    cy.get('h1, h2').should('have.length.at.least', 3);
  });

  it('menciona a LGPD ou Lei Geral de Proteção de Dados', () => {
    cy.contains(/LGPD|Lei Geral de Proteção/i).should('exist');
  });

  it('contém informações de contato do responsável', () => {
    cy.contains(/contato|e-mail|dpo/i).should('exist');
  });

  it('exibe a versão da política', () => {
    cy.contains('1.0').should('exist');
  });

});
