/// <reference types="cypress" />

describe('07 — Painel Administrativo', () => {

  it('Admin — lista de profissionais carrega', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/professionals');
    cy.contains('Dra. Ana Souza', { timeout: 10000 }).should('exist');
    cy.contains('Dr. Carlos Menezes', { timeout: 10000 }).should('exist');
  });

  it('Admin — lista de pacientes carrega', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/patients');
    cy.contains('Lucas Silva', { timeout: 10000 }).should('exist');
    cy.contains('Maria Oliveira', { timeout: 10000 }).should('exist');
    cy.contains('João Pereira', { timeout: 10000 }).should('exist');
  });

  it('Admin — agenda geral carrega', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/appointments');
    // A agenda geral deve conter consultas de ambos profissionais
    cy.get('[class*="border"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

});
