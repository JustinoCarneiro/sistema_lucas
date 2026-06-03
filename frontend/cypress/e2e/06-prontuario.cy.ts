/// <reference types="cypress" />

describe('06 — Prontuários (Médical Record)', () => {

  const consulta = {
    id: 6,
    patientId: 3,
    patientName: 'Lucas Silva',
    professionalName: 'Dra. Ana Souza',
    startTime: '2026-05-19T09:00:00',
    reason: 'Sessão semanal',
    status: 'CONFIRMADA'
  };

  const historico = [
    {
      id: 1,
      criadoEm: '2026-05-05T10:00:00',
      notas: 'Primeira sessão. Paciente relata ansiedade generalizada há aproximadamente 8 meses.',
      professional: { name: 'Dra. Ana Souza' }
    },
    {
      id: 2,
      criadoEm: '2026-05-12T10:00:00',
      notas: 'Segunda sessão. Leve melhora na qualidade do sono.',
      professional: { name: 'Dra. Ana Souza' }
    }
  ];

  context('Acesso ao Prontuário', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/consultas/6', {
        statusCode: 200,
        body: consulta
      }).as('getConsulta');
      cy.intercept('GET', '**/prontuarios/paciente/3', {
        statusCode: 200,
        body: historico
      }).as('getHistorico');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/medical-record/6');
      cy.wait(['@getConsulta', '@getHistorico']);
    });

    it('exibe título e nome do paciente da consulta', () => {
      cy.contains('h1', 'Prontuário').should('be.visible');
      cy.contains('span', 'Lucas Silva').should('be.visible');
    });

    it('exibe campo de anotações com placeholder correto', () => {
      cy.contains('h2', 'Anotações do atendimento').should('be.visible');
      cy.get('textarea')
        .should('have.attr', 'placeholder')
        .and('match', /Descreva as observações/);
    });

    it('exibe histórico de atendimentos anteriores', () => {
      cy.contains('h2', 'Histórico de atendimentos').should('be.visible');
      cy.contains('Primeira sessão').should('be.visible');
      cy.contains('Segunda sessão').should('be.visible');
      cy.contains('span', 'Dra. Ana Souza').should('exist');
    });

    it('mostra mensagem padrão quando não há histórico', () => {
      cy.intercept('GET', '**/prontuarios/paciente/3', {
        statusCode: 200,
        body: []
      }).as('getVazio');
      cy.visit('/panel/medical-record/6');
      cy.wait('@getVazio');
      cy.contains('Nenhum atendimento anterior registrado').should('be.visible');
    });
  });

  context('Salvar Prontuário', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/consultas/6', {
        statusCode: 200,
        body: consulta
      }).as('getConsulta');
      cy.intercept('GET', '**/prontuarios/paciente/3', {
        statusCode: 200,
        body: historico
      }).as('getHistorico');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/medical-record/6');
      cy.wait(['@getConsulta', '@getHistorico']);
    });

    it('avisa quando tentar salvar sem anotações', () => {
      cy.contains('button', 'Salvar e finalizar atendimento').click();
      cy.contains('[role="alert"]', /preencha as anotações/).should('be.visible');
    });

    it('envia POST /prontuarios e navega para agenda do profissional', () => {
      cy.intercept('POST', '**/prontuarios', {
        statusCode: 201,
        body: { id: 99 }
      }).as('postProntuario');
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: []
      }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: []
      }).as('getTodas');
      const notas = 'Paciente apresentou melhora significativa. Manter conduta atual.';
      cy.get('textarea').type(notas);
      cy.contains('button', 'Salvar e finalizar atendimento').click();

      cy.wait('@postProntuario').then(({ request }) => {
        expect(request.body.notas).to.eq(notas);
        expect(String(request.body.appointmentId)).to.eq('6');
      });
      cy.contains('[role="alert"]', /Prontuário salvo/).should('be.visible');
      cy.url().should('include', '/panel/professional-appointments');
    });

    it('mostra erro quando POST falha', () => {
      cy.intercept('POST', '**/prontuarios', {
        statusCode: 400,
        body: { message: 'Notas inválidas' }
      }).as('postProntuarioErro');
      cy.get('textarea').type('Tentativa de anotação.');
      cy.contains('button', 'Salvar e finalizar atendimento').click();

      cy.wait('@postProntuarioErro');
      cy.contains('[role="alert"]', /Erro ao salvar prontuário/).should('be.visible');
    });
  });

  context('Acesso pelo profissional via agenda', () => {
    it('botão "Iniciar atendimento" navega para o prontuário da consulta', () => {
      const consultasHoje = [
        {
          id: 6,
          patientName: 'Lucas Silva',
          professionalName: 'Dra. Ana Souza',
          startTime: '2026-05-19T09:00:00',
          reason: 'Sessão semanal',
          status: 'CONFIRMADA'
        }
      ];

      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: consultasHoje
      }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: []
      }).as('getTodas');
      cy.intercept('GET', '**/consultas/6', {
        statusCode: 200,
        body: consulta
      }).as('getConsulta');
      cy.intercept('GET', '**/prontuarios/paciente/3', {
        statusCode: 200,
        body: historico
      }).as('getHistorico');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/professional-appointments');
      cy.wait('@getHoje');

      cy.contains('button', 'Iniciar atendimento').click();
      cy.url().should('match', /\/panel\/medical-record\/6$/);
      cy.contains('h1', 'Prontuário').should('be.visible');
    });
  });

});
