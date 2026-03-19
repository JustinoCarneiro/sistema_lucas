/// <reference types="cypress" />

describe('08 — Segurança', () => {

  it('Rota protegida redireciona para login sem token', () => {
    // Limpa qualquer sessão anterior
    cy.clearLocalStorage();
    cy.visit('/panel/dashboard');
    // Deve redirecionar para login pois não há token
    cy.url({ timeout: 10000 }).should('include', '/login');
  });

  it('API rejeita requisição sem Authorization header', () => {
    cy.request({
      method: 'GET',
      url: 'http://localhost:8081/professionals/me',
      failOnStatusCode: false
    }).then((response) => {
      // Sem token, deve retornar 403 Forbidden
      expect(response.status).to.be.oneOf([401, 403]);
    });
  });

  it('Rate Limiting — bloqueia após excesso de tentativas', () => {
    // O bucket permite 30 req/min com refill greedy, mas como cy.request
    // é sequencial (~0.3s cada), durante a execução alguns tokens são recarregados.
    // Enviamos 50 requisições para garantir o estouro mesmo com refill parcial.
    const sendRequest = () =>
      cy.request({
        method: 'POST',
        url: 'http://localhost:8081/auth/login',
        body: { email: 'fake@test.com', password: 'wrong' },
        failOnStatusCode: false
      });

    for (let i = 0; i < 50; i++) {
      sendRequest();
    }

    // Após 50 requisições, a próxima DEVE retornar 429
    sendRequest().then((response) => {
      expect(response.status).to.eq(429);
    });
  });

});
