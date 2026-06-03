/// <reference types="cypress" />

describe('19 — Consultas Atrasadas (Dashboard + Aba Atrasadas)', () => {

  const atrasadas = [
    {
      id: 101, patientId: 3, patientName: 'João Silva',
      professionalName: 'Dra. Ana',
      startTime: '2026-05-10T09:00:00',
      status: 'AGENDADA',
      reason: 'Rotina',
    },
    {
      id: 102, patientId: 4, patientName: 'Maria Souza',
      professionalName: 'Dra. Ana',
      startTime: '2026-05-11T14:00:00',
      status: 'CONFIRMADA',
      reason: 'Acompanhamento',
    }
  ];

  const dashboardComAtrasadas = {
    consultasHoje: 1,
    pendentesConfirmacao: 0,
    consultasAtrasadas: 2,
    proximasConsultas: [],
    prontuariosRecentes: [],
    documentosRecentes: [],
  };

  const dashboardSemAtrasadas = {
    consultasHoje: 1,
    pendentesConfirmacao: 0,
    consultasAtrasadas: 0,
    proximasConsultas: [],
    prontuariosRecentes: [],
    documentosRecentes: [],
  };

  context('Dashboard — Banner de Urgência', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: dashboardComAtrasadas,
      }).as('getDashProf');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/dashboard');
      cy.wait('@getDashProf');
    });

    it('banner vermelho aparece quando consultasAtrasadas > 0', () => {
      cy.contains('Ação urgente necessária').should('be.visible');
    });

    it('banner exibe contagem de consultas atrasadas', () => {
      cy.contains('2 consulta(s) com data passada').should('be.visible');
    });

    it('link "Ver atrasadas" navega para /panel/professional-appointments', () => {
      cy.intercept('GET', '**/consultas/profissional/hoje', { statusCode: 200, body: [] }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', { statusCode: 200, body: [] }).as('getTodas');
      cy.intercept('GET', '**/consultas/profissional/atrasadas', { statusCode: 200, body: atrasadas }).as('getAtrasadas');

      cy.contains('a', 'Ver atrasadas').click();
      cy.url().should('include', '/panel/professional-appointments');
    });
  });

  context('Dashboard — Sem Atrasadas', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: dashboardSemAtrasadas,
      }).as('getDashProf');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/dashboard');
      cy.wait('@getDashProf');
    });

    it('banner vermelho NÃO aparece quando consultasAtrasadas = 0', () => {
      cy.contains('Ação urgente necessária').should('not.exist');
    });
  });

  context('Aba Atrasadas — /panel/professional-appointments', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/consultas/profissional/hoje', { statusCode: 200, body: [] }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', { statusCode: 200, body: [] }).as('getTodas');
      cy.intercept('GET', '**/consultas/profissional/atrasadas', {
        statusCode: 200,
        body: atrasadas,
      }).as('getAtrasadas');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/professional-appointments');
      cy.wait('@getAtrasadas');
    });

    it('aba "Atrasadas" está visível na navegação', () => {
      cy.contains('button', 'Atrasadas').should('be.visible');
    });

    it('clicar na aba Atrasadas exibe a lista de consultas atrasadas', () => {
      cy.contains('button', 'Atrasadas').click();
      cy.contains('João Silva').should('be.visible');
      cy.contains('Maria Souza').should('be.visible');
    });

    it('banner de aviso "Estas consultas têm data passada" é visível na aba', () => {
      cy.contains('button', 'Atrasadas').click();
      cy.contains('data passada').should('be.visible');
    });

    it('"Registrar atendimento" presente para consulta AGENDADA', () => {
      cy.contains('button', 'Atrasadas').click();
      cy.contains('João Silva').parents('tr, div.p-5').first().within(() => {
        cy.contains('button', 'Registrar').should('exist');
      });
    });

    it('"Paciente faltou" chama PATCH /falta e recarrega atrasadas', () => {
      cy.intercept('PATCH', '**/consultas/101/falta', {
        statusCode: 200, body: 'ok'
      }).as('patchFalta');
      cy.intercept('GET', '**/consultas/profissional/atrasadas', {
        statusCode: 200,
        body: atrasadas.filter(c => c.id !== 101),
      }).as('reloadAtrasadas');
      cy.on('window:confirm', () => true);

      cy.contains('button', 'Atrasadas').click();
      cy.contains('João Silva').parents('tr, div.p-5').first().within(() => {
        cy.contains('button', 'faltou').click();
      });

      cy.wait('@patchFalta');
      cy.wait('@reloadAtrasadas');
    });

    it('estado vazio mostra "Nenhuma consulta atrasada" quando lista é vazia', () => {
      cy.intercept('GET', '**/consultas/profissional/atrasadas', {
        statusCode: 200,
        body: [],
      }).as('getVazia');
      cy.intercept('GET', '**/consultas/profissional/hoje', { statusCode: 200, body: [] });
      cy.intercept('GET', '**/consultas/profissional/todas', { statusCode: 200, body: [] });

      cy.visit('/panel/professional-appointments');
      cy.wait('@getVazia');
      cy.contains('button', 'Atrasadas').click();
      cy.contains('Nenhuma consulta atrasada').should('be.visible');
    });
  });

});
