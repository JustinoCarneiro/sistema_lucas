/// <reference types="cypress" />

describe('08 — Segurança', () => {

  // ─── JWTs sintéticos para os testes de guarda ────────────────────────
  // Não precisam ser assinados — o auth.guard apenas decodifica e checa exp
  function makeFakeJwt(payload: Record<string, any>): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body   = btoa(JSON.stringify(payload));
    return `${header}.${body}.assinatura-falsa`;
  }

  context('Guarda de rotas (authGuard)', () => {
    it('sem token redireciona para /login ao tentar /panel/*', () => {
      cy.clearLocalStorage();
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.contains('button', /Entrar no sistema/).should('be.visible');
    });

    it('token expirado redireciona para /login e é removido do storage', () => {
      const expired = makeFakeJwt({
        sub: 'lucas@email.com',
        role: 'PATIENT',
        verified: true,
        exp: Math.floor(Date.now() / 1000) - 60 // expirou há 1 minuto
      });
      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('token', expired);
        }
      });
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'token').should('be.null');
    });

    it('token malformado redireciona para /login', () => {
      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('token', 'isso-nao-eh-um-jwt-valido');
        }
      });
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'token').should('be.null');
    });

    it('token válido permite acesso ao /panel/dashboard', () => {
      const valid = makeFakeJwt({
        sub: 'lucas@email.com',
        role: 'PATIENT',
        verified: true,
        exp: Math.floor(Date.now() / 1000) + 3600
      });
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getDash');
      cy.intercept('GET', 'http://localhost:8081/patients/me', {
        statusCode: 200,
        body: { id: 3, name: 'Lucas Silva', email: 'lucas@email.com' }
      }).as('getMe');

      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('token', valid);
        }
      });
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/panel/dashboard');
      cy.wait('@getDash');
    });
  });

  context('Login UI', () => {
    it('mostra erro quando o backend rejeita credenciais', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 401,
        body: { message: 'E-mail ou senha inválidos.' }
      }).as('postLogin');

      cy.visit('/login');
      cy.get('input#email').type('errado@clinica.com');
      cy.get('input#password').type('senhaErrada');
      cy.get('button[type="submit"]').click();

      cy.wait('@postLogin');
      cy.contains(/E-mail ou senha inválidos/i).should('be.visible');
      cy.url().should('include', '/login');
    });

    it('botão Entrar fica desabilitado quando o formulário é inválido', () => {
      cy.visit('/login');
      cy.get('button[type="submit"]').should('be.disabled');
      cy.get('input#email').type('email-invalido').blur();
      cy.contains('E-mail inválido').should('be.visible');
      cy.get('input#password').type('xx');
      cy.get('button[type="submit"]').should('be.disabled');
    });
  });

  context('Auth interceptor', () => {
    it('anexa header Authorization: Bearer <token> em requisições não-/auth', () => {
      const valid = makeFakeJwt({
        sub: 'ana@clinica.com',
        role: 'PROFESSIONAL',
        verified: true,
        exp: Math.floor(Date.now() / 1000) + 3600
      });

      cy.intercept('GET', '**/dashboard/profissional', (req) => {
        expect(req.headers.authorization).to.eq(`Bearer ${valid}`);
        req.reply({
          statusCode: 200,
          body: {
            consultasHoje: [],
            proximasConsultas: [],
            totalPacientes: 0,
            ultimosProntuarios: [],
            documentosRecentes: []
          }
        });
      }).as('getDashWithHeader');

      cy.intercept('GET', 'http://localhost:8081/professionals/me', {
        statusCode: 200,
        body: { id: 1, name: 'Dra. Ana Souza', email: 'ana@clinica.com' }
      }).as('getMe');

      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('token', valid);
        }
      });
      cy.visit('/panel/dashboard');
      cy.wait('@getDashWithHeader');
    });
  });

  context('Backend — endpoints protegidos', () => {
    it('API rejeita GET /professionals/me sem Authorization', () => {
      cy.request({
        method: 'GET',
        url: `${Cypress.env('apiUrl')}/professionals/me`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([401, 403]);
      });
    });

    it('API rejeita GET /patients sem Authorization', () => {
      cy.request({
        method: 'GET',
        url: `${Cypress.env('apiUrl')}/patients`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([401, 403]);
      });
    });
  });

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
