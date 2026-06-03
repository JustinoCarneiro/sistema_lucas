/// <reference types="cypress" />

describe('14 — Recuperação de Senha', () => {

  context('Forgot Password — /forgot-password', () => {
    beforeEach(() => {
      cy.visit('/forgot-password');
    });

    it('página carrega com campo email', () => {
      cy.get('input[formControlName="email"]').should('be.visible');
    });

    it('botão enviar desabilitado com email vazio', () => {
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('botão habilitado com email válido preenchido', () => {
      cy.get('input[formControlName="email"]').type('user@test.com');
      cy.get('button[type="submit"]').should('not.be.disabled');
    });

    it('POST 200 → exibe mensagem de sucesso', () => {
      cy.intercept('POST', '**/auth/esqueci-senha', {
        statusCode: 200,
        body: 'Se o e-mail existir, você receberá as instruções.'
      }).as('postForgot');

      cy.get('input[formControlName="email"]').type('user@test.com');
      cy.get('button[type="submit"]').click();
      cy.wait('@postForgot');

      cy.contains('instruções').should('be.visible');
    });

    it('POST 404 → ainda exibe mensagem de sucesso (não revela existência do e-mail)', () => {
      cy.intercept('POST', '**/auth/esqueci-senha', {
        statusCode: 404,
        body: { message: 'Usuário não encontrado.' }
      }).as('postNotFound');

      cy.get('input[formControlName="email"]').type('naoexiste@test.com');
      cy.get('button[type="submit"]').click();
      cy.wait('@postNotFound');

      cy.contains('instruções').should('be.visible');
    });
  });

  context('Reset Password — /reset-password?token=xxx', () => {
    beforeEach(() => {
      cy.visit('/reset-password?token=token-valido-123');
    });

    it('página carrega com campos de nova senha e confirmação', () => {
      cy.get('input[formControlName="novaSenha"]').should('be.visible');
      cy.get('input[formControlName="confirmarSenha"]').should('be.visible');
    });

    it('botão desabilitado quando senha muito curta', () => {
      cy.get('input[formControlName="novaSenha"]').type('12345');
      cy.get('input[formControlName="confirmarSenha"]').type('12345');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('botão desabilitado quando senhas não coincidem', () => {
      cy.get('input[formControlName="novaSenha"]').type('senha123');
      cy.get('input[formControlName="confirmarSenha"]').type('diferente');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('POST 200 → exibe mensagem de sucesso', () => {
      cy.intercept('POST', '**/auth/redefinir-senha', {
        statusCode: 200,
        body: 'Senha redefinida com sucesso!'
      }).as('postReset');

      cy.get('input[formControlName="novaSenha"]').type('novaSenha123');
      cy.get('input[formControlName="confirmarSenha"]').type('novaSenha123');
      cy.get('button[type="submit"]').click();
      cy.wait('@postReset');

      cy.contains('Senha redefinida!').should('be.visible');
    });

    it('POST 400 (token inválido) → exibe mensagem de erro', () => {
      cy.intercept('POST', '**/auth/redefinir-senha', {
        statusCode: 400,
        body: 'Link inválido ou expirado. Solicite um novo.'
      }).as('postInvalid');

      cy.get('input[formControlName="novaSenha"]').type('novaSenha123');
      cy.get('input[formControlName="confirmarSenha"]').type('novaSenha123');
      cy.get('button[type="submit"]').click();
      cy.wait('@postInvalid');

      cy.contains('inválido').should('be.visible');
    });
  });

  context('Reset Password — sem token na URL', () => {
    it('exibe mensagem de erro quando não há token', () => {
      cy.visit('/reset-password');
      cy.contains('inválido').should('be.visible');
    });
  });

});
