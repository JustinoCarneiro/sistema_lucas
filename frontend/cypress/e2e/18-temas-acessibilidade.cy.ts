/// <reference types="cypress" />

describe('18 — Temas e Acessibilidade', () => {

  context('Toggle de Tema Claro/Escuro', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 0, totalPacientes: 0, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getDash');

      cy.login('admin@clinica.com', 'admin');
      cy.visit('/panel/dashboard');
      cy.wait('@getDash');
    });

    it('botão de toggle de tema existe no painel', () => {
      cy.get('[data-testid="theme-toggle"], button[aria-label*="tema"], button[title*="tema"]')
        .should('exist');
    });

    it('toggle de tema altera a classe dark no elemento html', () => {
      cy.document().then(doc => {
        const hasDark = doc.documentElement.classList.contains('dark');

        cy.get('[data-testid="theme-toggle"], button[aria-label*="tema"], button[title*="tema"]')
          .first().click();

        cy.document().then(docAfter => {
          const hasDarkAfter = docAfter.documentElement.classList.contains('dark');
          expect(hasDarkAfter).not.to.eq(hasDark);
        });
      });
    });

    it('tema persiste após reload (salvo em localStorage)', () => {
      cy.document().then(doc => {
        const darkBefore = doc.documentElement.classList.contains('dark');

        cy.get('[data-testid="theme-toggle"], button[aria-label*="tema"], button[title*="tema"]')
          .first().click();

        cy.reload();

        cy.document().then(docAfter => {
          const darkAfter = docAfter.documentElement.classList.contains('dark');
          expect(darkAfter).not.to.eq(darkBefore);
        });
      });
    });
  });

});
