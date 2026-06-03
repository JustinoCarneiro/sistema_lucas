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

    it('acesso restrito com cookie ausente/expirado (401) redireciona para /login e é removido do storage', () => {
      cy.intercept('GET', '**/dashboard/paciente', { statusCode: 401 }).as('getDash401');
      cy.intercept('POST', '**/auth/refresh', { statusCode: 401 }).as('refresh401');
      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('role', 'PATIENT');
        }
      });
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'role').should('be.null');
    });

    it('storage com role mas sem cookie (401) redireciona para /login', () => {
      cy.intercept('GET', '**/dashboard/profissional', { statusCode: 401 }).as('getDashProf401');
      cy.intercept('POST', '**/auth/refresh', { statusCode: 401 }).as('refresh401');
      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('role', 'PROFESSIONAL');
        }
      });
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'role').should('be.null');
    });

    it('sessão válida permite acesso ao /panel/dashboard', () => {
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
          win.localStorage.setItem('role', 'PATIENT');
          win.localStorage.setItem('verified', 'true');
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
    it('NÃO anexa header Authorization pois agora usa Cookie HttpOnly (SEC-01)', () => {
      cy.intercept('GET', '**/dashboard/profissional', (req) => {
        expect(req.headers.authorization).to.be.undefined;
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
          win.localStorage.setItem('role', 'PROFESSIONAL');
          win.localStorage.setItem('verified', 'true');
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


});
