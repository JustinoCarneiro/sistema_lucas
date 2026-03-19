/// <reference types="cypress" />

describe('02 — Dashboards por Role', () => {

  it('Dashboard do Admin — cards visíveis', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.url().should('include', '/panel/dashboard');
    cy.contains('Visão Geral', { timeout: 10000 }).should('be.visible');
    cy.contains('Profissionais cadastrados').should('be.visible');
    cy.contains('Pacientes cadastrados').should('be.visible');
    cy.contains('Consultas hoje').should('be.visible');
  });

  it('Dashboard do Admin — gráfico por status', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.contains('Consultas por status', { timeout: 10000 }).should('be.visible');
  });

  it('Dashboard do Profissional — painel carregado', () => {
    cy.login('ana@clinica.com', '123456');
    cy.contains('Meu Painel', { timeout: 10000 }).should('be.visible');
    cy.contains('Consultas hoje').should('be.visible');
    cy.contains('Próximas agendadas').should('be.visible');
    cy.contains('Pacientes ativos').should('be.visible');
  });

  it('Dashboard do Profissional — seções detalhadas', () => {
    cy.login('ana@clinica.com', '123456');
    cy.contains('Agenda de hoje', { timeout: 10000 }).should('be.visible');
    cy.contains('Últimos prontuários').should('be.visible');
    cy.contains('Documentos recentes').should('be.visible');
  });

  it('Dashboard do Paciente — painel carregado', () => {
    cy.login('lucas@email.com', '123456');
    cy.contains('Meu Painel', { timeout: 10000 }).should('be.visible');
    cy.contains('Consultas realizadas').should('be.visible');
    cy.contains('Consultas agendadas').should('be.visible');
    cy.contains('Documentos disponíveis').should('be.visible');
  });

  it('Dashboard do Paciente — dados do perfil resumido', () => {
    cy.login('lucas@email.com', '123456');
    cy.contains('Meus dados', { timeout: 10000 }).should('be.visible');
    cy.contains('Lucas Silva').should('exist');
    cy.contains('lucas@email.com').should('exist');
  });

});
