/// <reference types="cypress" />

describe('04 — Consultas (Agendamento, Confirmação, Cancelamento)', () => {

  it('Paciente — lista de consultas carrega', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    cy.contains('Minhas Consultas', { timeout: 10000 }).should('be.visible');
  });

  it('Paciente — consultas do Lucas aparecem na lista', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    // Lucas tem consultas com Dra. Ana e Dr. Carlos no DataInitializer
    cy.contains('Dra. Ana Souza', { timeout: 10000 }).should('exist');
  });

  it('Paciente — formulário de agendamento aparece ao clicar', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.contains('Nova consulta').should('be.visible');
    cy.get('select').should('exist'); // Select de profissionais
    cy.contains('Escolha o profissional').should('exist');
  });

  it('Paciente — consultas exibem labels de status', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    // Pelo DataInitializer, há consultas com status variados
    cy.get('[class*="rounded-full"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Profissional — agenda completa carrega', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    cy.contains('Lucas Silva', { timeout: 10000 }).should('exist');
  });

  it('Profissional — agenda exibe consultas de hoje', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    // Existem consultas de hoje no DataInitializer (c6, c7)
    cy.get('[class*="rounded-full"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Admin — agenda geral carrega', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/appointments');
    // Admin vê consultas de todos
    cy.get('table, [class*="border"]', { timeout: 10000 }).should('exist');
  });

});
