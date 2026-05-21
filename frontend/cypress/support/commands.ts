/// <reference types="cypress" />

// Login via UI (usa backend real — para testes de integração)
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input#email').type(email);
  cy.get('input#password').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/panel', { timeout: 10000 });
});

// Login programático — injeta JWT sintético no localStorage sem hit de backend.
// Usar em testes que validam UI/lógica frontend, não o fluxo de autenticação.
Cypress.Commands.add('loginProgrammatic', (role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT', email: string) => {
  const header  = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({
    sub: email,
    role,
    verified: true,
    exp: Math.floor(Date.now() / 1000) + 3600,
  }));
  cy.window().then(win => win.localStorage.setItem('token', `${header}.${payload}.fake-sig`));
});

// Stub padrão do dashboard por role — evita repetição em beforeEach.
Cypress.Commands.add('interceptDashboard', (role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT') => {
  if (role === 'PROFESSIONAL') {
    cy.intercept('GET', '**/dashboard/profissional', {
      statusCode: 200,
      body: {
        consultasHoje: 2,
        pendentesConfirmacao: 1,
        consultasAtrasadas: 0,
        totalPacientes: 10,
      },
    }).as('dashProf');
  } else if (role === 'PATIENT') {
    cy.intercept('GET', '**/dashboard/paciente', {
      statusCode: 200,
      body: { totalRealizadas: 3, totalAgendadas: 1, documentosDisponiveis: [], perfil: {} },
    }).as('dashPac');
  } else {
    cy.intercept('GET', '**/dashboard/admin', {
      statusCode: 200,
      body: { totalPacientes: 50, totalProfissionais: 5, consultasHoje: 8 },
    }).as('dashAdmin');
  }
});

declare namespace Cypress {
  interface Chainable {
    login(email: string, password: string): Chainable<void>;
    loginProgrammatic(role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT', email: string): Chainable<void>;
    interceptDashboard(role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT'): Chainable<void>;
  }
}
