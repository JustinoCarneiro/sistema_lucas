/// <reference types="cypress" />

describe('15 — Verificação de E-mail', () => {

  it('token válido → GET 200 → exibe mensagem de sucesso', () => {
    cy.intercept('GET', '**/auth/verify*', {
      statusCode: 200,
      body: 'E-mail verificado com sucesso!'
    }).as('verify');

    cy.visit('/verify-email?token=token-valido');
    cy.wait('@verify');

    cy.contains('sucesso').should('be.visible');
  });

  it('token expirado → GET 400 → exibe mensagem de erro', () => {
    cy.intercept('GET', '**/auth/verify*', {
      statusCode: 400,
      body: { message: 'Token expirado ou inválido.' }
    }).as('verifyExpired');

    cy.visit('/verify-email?token=token-expirado');
    cy.wait('@verifyExpired');

    cy.contains('expirado').should('be.visible');
  });

  it('sem token na URL → exibe mensagem de token não encontrado', () => {
    cy.visit('/verify-email');
    cy.contains('Token de verificação não encontrado').should('be.visible');
  });

});
