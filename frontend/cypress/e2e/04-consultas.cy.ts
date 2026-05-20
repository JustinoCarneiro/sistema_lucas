/// <reference types="cypress" />

describe('04 — Consultas (Agendamento, Confirmação, Cancelamento)', () => {

  context('Visão do Paciente', () => {
    const minhasConsultas = [
      {
        id: 1,
        patientId: 3,
        professionalName: 'Dra. Ana Souza',
        patientName: 'Lucas Silva',
        startTime: '2026-05-04T10:00:00',
        reason: 'Avaliação inicial',
        cancelReason: null,
        status: 'CONCLUIDA',
        podeCancelar: false,
        atrasada: true
      },
      {
        id: 10,
        patientId: 3,
        professionalName: 'Dra. Ana Souza',
        patientName: 'Lucas Silva',
        startTime: '2026-06-15T10:00:00',
        reason: 'Sessão de acompanhamento',
        cancelReason: null,
        status: 'AGENDADA',
        podeCancelar: true,
        atrasada: false
      },
      {
        id: 12,
        patientId: 3,
        professionalName: 'Dr. Carlos Menezes',
        patientName: 'Lucas Silva',
        startTime: '2026-06-22T09:00:00',
        reason: 'Revisão de medicação',
        cancelReason: null,
        status: 'CONFIRMADA_PROFISSIONAL',
        podeCancelar: true,
        atrasada: false
      },
      {
        id: 39,
        patientId: 3,
        professionalName: 'Dra. Ana Souza',
        patientName: 'Lucas Silva',
        startTime: '2026-05-19T18:00:00',
        reason: 'Sessão extra',
        cancelReason: null,
        status: 'CONFIRMADA',
        podeCancelar: true,
        atrasada: true
      }
    ];

    const profissionaisDisp = [
      { id: 1, name: 'Dra. Ana Souza', specialty: 'Psicologia Clínica' },
      { id: 2, name: 'Dr. Carlos Menezes', specialty: 'Psiquiatria' }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: minhasConsultas
      }).as('getMinhas');
      cy.intercept('GET', '**/disponibilidade/profissionais-disponiveis', {
        statusCode: 200,
        body: profissionaisDisp
      }).as('getProfs');
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [] }
      }).as('getPatDash');
      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhas');
    });

    it('exibe título e lista de consultas', () => {
      cy.contains('h1', 'Minhas Consultas').should('be.visible');
      cy.contains('Dra. Ana Souza').should('be.visible');
      cy.contains('Dr. Carlos Menezes').should('be.visible');
    });

    it('exibe labels de status para cada consulta', () => {
      cy.contains('Concluída').should('be.visible');
      cy.contains('Agendada').should('be.visible');
      cy.contains('Aguardando paciente').should('be.visible');
      cy.contains('Confirmada').should('be.visible');
    });

    it('exibe aviso de ação tardia quando atrasada=true e status ativo', () => {
      cy.contains('⚠️ Ação tardia:').should('be.visible');
    });

    it('formulário de agendamento abre com select de profissionais', () => {
      cy.contains('button', '+ Agendar consulta').click();
      cy.contains('h2', 'Nova consulta').should('be.visible');
      cy.contains('label', '1. Escolha o profissional').should('be.visible');
      cy.get('select[formControlName="professionalId"]').should('exist');
      cy.get('select[formControlName="professionalId"]')
        .find('option').contains('Dra. Ana Souza').should('exist');
    });

    it('modal de cancelamento abre e exige justificativa de 10+ caracteres', () => {
      cy.get('[data-testid="cancelar-button"]').first().click();
      cy.contains('h3', 'Cancelar Consulta').should('be.visible');
      cy.contains('button', 'Confirmar Cancelamento').should('be.disabled');

      cy.get('textarea[formControlName="justification"]').type('Curta');
      cy.contains('button', 'Confirmar Cancelamento').should('be.disabled');

      cy.get('textarea[formControlName="justification"]')
        .clear().type('Justificativa com mais de 10 caracteres');
      cy.contains('button', 'Confirmar Cancelamento').should('not.be.disabled');

      cy.contains('button', 'Voltar').click();
      cy.contains('h3', 'Cancelar Consulta').should('not.exist');
    });

    it('envia POST de cancelamento ao confirmar com justificativa válida', () => {
      cy.intercept('POST', '**/consultas/*/cancelar', {
        statusCode: 200,
        body: 'OK'
      }).as('postCancel');

      cy.get('[data-testid="cancelar-button"]').first().click();
      cy.get('textarea[formControlName="justification"]')
        .type('Imprevisto no horário, preciso cancelar');
      cy.contains('button', 'Confirmar Cancelamento').click();

      cy.wait('@postCancel').its('request.body.justification')
        .should('contain', 'Imprevisto');
    });

    it('modal de reagendamento abre com seleção de data', () => {
      cy.intercept('GET', '**/disponibilidade/*/working-days', {
        statusCode: 200,
        body: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
      }).as('getDays');

      cy.get('[data-testid="reagendar-button"]').first().click();
      cy.contains('h3', 'Reagendar Consulta').should('be.visible');
      cy.get('select[formControlName="date"]').should('exist');

      cy.contains('button', 'Voltar').click();
      cy.contains('h3', 'Reagendar Consulta').should('not.exist');
    });
  });

  context('Visão do Profissional', () => {
    const consultasHoje = [
      {
        id: 6,
        patientName: 'Lucas Silva',
        professionalName: 'Dra. Ana Souza',
        startTime: '2026-05-19T09:00:00',
        reason: 'Sessão semanal',
        status: 'CONFIRMADA',
        podeCancelar: false,
        atrasada: false
      },
      {
        id: 7,
        patientName: 'Maria Oliveira',
        professionalName: 'Dra. Ana Souza',
        startTime: '2026-05-19T11:00:00',
        reason: 'Retorno',
        status: 'AGENDADA',
        podeCancelar: false,
        atrasada: false
      }
    ];

    const todasConsultas = [
      {
        id: 9,
        patientName: 'Lucas Silva',
        professionalName: 'Dra. Ana Souza',
        startTime: '2026-05-26T10:00:00',
        reason: 'Acompanhamento',
        status: 'AGENDADA',
        podeCancelar: true,
        atrasada: false
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: consultasHoje
      }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: todasConsultas
      }).as('getTodas');
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/professional-appointments');
      cy.wait('@getHoje');
    });

    it('exibe título e aba Hoje selecionada por padrão', () => {
      cy.contains('h1', 'Minha Agenda').should('be.visible');
      cy.contains('button', 'Hoje').should('have.class', 'border-blue-900');
    });

    it('lista consultas de hoje com pacientes', () => {
      cy.contains('Lucas Silva').should('be.visible');
      cy.contains('Maria Oliveira').should('be.visible');
      cy.contains('Sessão semanal').should('be.visible');
    });

    it('exibe botão Confirmar para consultas com status AGENDADA', () => {
      cy.contains('Maria Oliveira').parents('.bg-white')
        .contains('button', 'Confirmar').should('be.visible');
    });

    it('exibe botão "Iniciar atendimento" para consultas confirmadas', () => {
      cy.contains('Lucas Silva').parents('.bg-white')
        .contains('button', 'Iniciar atendimento').should('be.visible');
    });

    it('troca para aba Próximas e mostra agendamentos futuros', () => {
      cy.contains('button', 'Próximas').click();
      cy.wait('@getTodas');
      cy.contains('Acompanhamento').should('be.visible');
    });
  });

  context('Visão do Admin', () => {
    const consultas = [
      {
        id: 1,
        patientName: 'Lucas Silva',
        professionalName: 'Dra. Ana Souza',
        startTime: '2026-05-26T10:00:00',
        reason: 'Acompanhamento',
        status: 'AGENDADA'
      },
      {
        id: 2,
        patientName: 'Maria Oliveira',
        professionalName: 'Dr. Carlos Menezes',
        startTime: '2026-05-27T14:00:00',
        reason: 'Retorno',
        status: 'CONFIRMADA'
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/consultas', {
        statusCode: 200,
        body: consultas
      }).as('getConsultas');
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 0, totalPacientes: 0, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getAdminDash');
      cy.login('admin@clinica.com', 'admin');
      cy.visit('/panel/appointments');
      cy.wait('@getConsultas');
    });

    it('exibe título e tabela da agenda geral', () => {
      cy.contains('h1', 'Agenda Geral').should('be.visible');
      cy.get('table').should('exist');
      cy.contains('th', 'Paciente').should('be.visible');
      cy.contains('th', 'Profissional').should('be.visible');
      cy.contains('th', 'Status').should('be.visible');
    });

    it('lista consultas com pacientes e profissionais', () => {
      cy.contains('td', 'Lucas Silva').should('be.visible');
      cy.contains('td', 'Maria Oliveira').should('be.visible');
      cy.contains('td', 'Dra. Ana Souza').should('be.visible');
      cy.contains('td', 'Dr. Carlos Menezes').should('be.visible');
    });

    it('exibe botão Cancelar para consultas ativas', () => {
      cy.contains('td', 'Lucas Silva').parent('tr')
        .contains('button', 'Cancelar').should('be.visible');
    });
  });

});
