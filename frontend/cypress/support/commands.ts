/// <reference types="cypress" />

// Custom command para login reutilizável em todos os testes
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input#email').type(email);
  cy.get('input#password').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/panel', { timeout: 10000 });
});

// Declaração de tipos para TypeScript
declare namespace Cypress {
  interface Chainable {
    login(email: string, password: string): Chainable<void>;
  }
}
