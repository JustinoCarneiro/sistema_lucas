/// <reference types="cypress" />

describe('01 — Autenticação', () => {

  // ─── Testes negativos PRIMEIRO (antes de consumir o rate limit) ─────────

  it('Deve rejeitar senha errada', () => {
    cy.visit('/login');
    cy.get('input#email').type('lucas@email.com');
    cy.get('input#password').type('senhaerrada');
    cy.get('button[type="submit"]').click();
    // O componente exibe 'E-mail ou senha inválidos.' como fallback
    cy.get('div[class*="bg-red"]', { timeout: 8000 }).should('be.visible');
    cy.url().should('include', '/login');
  });

  it('Deve rejeitar e-mail inexistente', () => {
    cy.visit('/login');
    cy.get('input#email').type('naoexiste@fake.com');
    cy.get('input#password').type('123456');
    cy.get('button[type="submit"]').click();
    cy.get('div[class*="bg-red"]', { timeout: 8000 }).should('be.visible');
  });

  // ─── Navegação (não dispara POST para /auth) ───────────────────────────

  it('Deve navegar para tela de registro', () => {
    cy.visit('/login');
    cy.contains('Crie a sua conta aqui').click();
    cy.url().should('include', '/register');
    cy.contains('Crie sua Conta').should('be.visible');
  });

  it('Deve navegar para tela de esqueci senha', () => {
    cy.visit('/login');
    cy.contains('Esqueceu a senha?').click();
    cy.url().should('include', '/forgot-password');
  });

  // ─── Logins positivos ─────────────────────────────────────────────────

  it('Deve logar como PACIENTE (Lucas)', () => {
    cy.login('lucas@email.com', '123456');
    cy.url().should('include', '/panel/dashboard');
  });

  it('Deve logar como PROFISSIONAL (Dra. Ana)', () => {
    cy.login('ana@clinica.com', '123456');
    cy.url().should('include', '/panel/dashboard');
  });

  it('Deve logar como ADMIN', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.url().should('include', '/panel/dashboard');
  });

  it('Deve fazer logout corretamente', () => {
    cy.login('lucas@email.com', '123456');
    cy.contains('Sair do sistema', { timeout: 10000 }).scrollIntoView().click();
    cy.url().should('include', '/login');
  });

});
