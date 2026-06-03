/// <reference types="cypress" />

describe('99 — Rate Limiting de Segurança', () => {
  context('Backend — Rate Limiting (Bucket4j)', () => {
    it('bloqueia com 429 após exceder 30 req/min em /auth/login', () => {
      const sendBad = () =>
        cy.request({
          method: 'POST',
          url: `${Cypress.env('apiUrl')}/auth/login`,
          body: { email: 'bot@test.com', password: 'errado' },
          failOnStatusCode: false
        });

      // Dispara um número >>30 para estourar mesmo com refill greedy parcial
      const respostas: number[] = [];
      for (let i = 0; i < 45; i++) {
        sendBad().then((r) => respostas.push(r.status));
      }

      cy.then(() => {
        expect(respostas).to.include(429);
      });
    });
  });
});
