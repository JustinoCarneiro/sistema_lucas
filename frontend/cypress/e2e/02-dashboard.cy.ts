/// <reference types="cypress" />

describe('02 — Dashboards por Role', () => {

  context('Visão do Admin', () => {
    const adminDashboard = {
      totalProfissionais: 2,
      totalPacientes: 4,
      consultasHoje: 10,
      consultasPorStatus: {
        AGENDADA: 10,
        CONCLUIDA: 4,
        CANCELADA: 12,
        FALTA: 1
      }
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: adminDashboard
      }).as('getAdminDash');
      cy.login('admin@clinica.com', 'admin');
      cy.wait('@getAdminDash');
    });

    it('exibe título e cards de visão geral', () => {
      cy.contains('h1', 'Visão Geral').should('be.visible');
      cy.contains('p', 'Profissionais cadastrados').should('be.visible');
      cy.contains('p', 'Pacientes cadastrados').should('be.visible');
      cy.contains('p', 'Consultas hoje').should('be.visible');
    });

    it('exibe valores numéricos dos cards', () => {
      cy.contains('p', 'Profissionais cadastrados')
        .siblings('p').should('have.text', '2');
      cy.contains('p', 'Pacientes cadastrados')
        .siblings('p').should('have.text', '4');
      cy.contains('p', 'Consultas hoje')
        .siblings('p').should('have.text', '10');
    });

    it('exibe gráfico de consultas por status', () => {
      cy.contains('h2', 'Consultas por status').should('be.visible');
      cy.contains('span', 'Agendada').should('be.visible');
      cy.contains('span', 'Concluída').should('be.visible');
      cy.contains('span', 'Cancelada').should('be.visible');
    });

    it('exibe botões de exportação', () => {
      cy.contains('button', 'Exportar Pacientes').should('be.visible');
      cy.contains('button', 'Exportar Profissionais').should('be.visible');
      cy.contains('button', 'Exportar Relatório Geral').should('be.visible');
    });
  });

  context('Visão do Profissional', () => {
    const profDashboard = {
      consultasHoje: [
        {
          id: 1,
          patient: { id: 3, name: 'Lucas Silva' },
          dateTime: '2026-05-19T09:00:00',
          reason: 'Sessão semanal',
          status: 'CONFIRMADA'
        }
      ],
      proximasConsultas: [
        {
          id: 2,
          patient: { id: 3, name: 'Lucas Silva' },
          dateTime: '2026-05-26T10:00:00',
          status: 'AGENDADA'
        }
      ],
      totalPacientes: 3,
      ultimosProntuarios: [
        {
          id: 1,
          patient: { name: 'Lucas Silva' },
          criadoEm: '2026-05-12T10:00:00',
          notas: 'Sessão de acompanhamento.'
        }
      ],
      documentosRecentes: [
        {
          id: 1,
          titulo: 'Laudo Psicológico — Lucas Silva',
          paciente: { name: 'Lucas Silva' },
          criadoEm: '2026-05-12T10:00:00',
          disponivel: true
        }
      ],
      pendentesConfirmacao: 2
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: profDashboard
      }).as('getProfDash');
      cy.login('ana@clinica.com', '123456');
      cy.wait('@getProfDash');
    });

    it('exibe título e cards do painel profissional', () => {
      cy.contains('h1', 'Meu Painel').should('be.visible');
      cy.contains('p', 'Consultas hoje').should('be.visible');
      cy.contains('p', 'Próximas agendadas').should('be.visible');
      cy.contains('p', 'Pacientes ativos').should('be.visible');
    });

    it('exibe seções detalhadas (agenda, prontuários, documentos)', () => {
      cy.contains('h2', 'Agenda de hoje').should('be.visible');
      cy.contains('h2', 'Próximas consultas').should('be.visible');
      cy.contains('h2', 'Últimos prontuários').should('be.visible');
      cy.contains('h2', 'Documentos recentes').should('be.visible');
    });

    it('exibe pacientes da agenda mockada', () => {
      cy.contains('p', 'Lucas Silva').should('exist');
      cy.contains('p', 'Sessão de acompanhamento.').should('exist');
      cy.contains('p', 'Laudo Psicológico — Lucas Silva').should('exist');
    });

    it('exibe botão de exportar atendimentos', () => {
      cy.contains('button', 'Exportar Meus Atendimentos').should('be.visible');
    });

    it('exibe alerta de agendamentos aguardando aprovação', () => {
      cy.contains('Ação necessária').should('be.visible');
      cy.contains('Você tem 2 agendamento(s) aguardando sua aprovação.').should('be.visible');
      cy.contains('a', 'Ver agendamentos').should('have.attr', 'href', '/panel/professional-appointments');
    });
  });

  context('Visão do Paciente', () => {
    const patDashboard = {
      totalRealizadas: 3,
      totalAgendadas: 2,
      proximaConsulta: [
        {
          id: 19,
          professional: { id: 2, name: 'Dr. Carlos Menezes' },
          dateTime: '2026-05-22T13:00:00',
          reason: 'Retorno',
          status: 'CONFIRMADA'
        }
      ],
      perfil: {
        nome: 'Lucas Silva',
        email: 'lucas@email.com',
        telefone: '(11) 88888-9999',
        convenio: 'Particular'
      },
      documentosDisponiveis: [
        {
          id: 1,
          titulo: 'Laudo Psicológico — Lucas Silva',
          tipo: 'LAUDO_PSICOLOGICO',
          criadoEm: '2026-05-12T10:00:00',
          nomeProfissional: 'Dra. Ana Souza'
        }
      ],
      pendentesConfirmacao: 1
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: patDashboard
      }).as('getPatDash');
      cy.login('lucas@email.com', '123456');
      cy.wait('@getPatDash');
    });

    it('exibe título e cards do painel do paciente', () => {
      cy.contains('h1', 'Meu Painel').should('be.visible');
      cy.contains('p', 'Consultas realizadas').should('be.visible');
      cy.contains('p', 'Consultas agendadas').should('be.visible');
      cy.contains('p', 'Documentos disponíveis').should('be.visible');
    });

    it('exibe próxima consulta com profissional e horário', () => {
      cy.contains('h2', 'Próxima consulta').should('be.visible');
      cy.contains('p', 'Dr. Carlos Menezes').should('be.visible');
      cy.contains('p', 'Motivo: Retorno').should('be.visible');
    });

    it('exibe dados do perfil resumido', () => {
      cy.contains('h2', 'Meus dados').should('be.visible');
      cy.contains('span', 'Lucas Silva').should('be.visible');
      cy.contains('span', 'lucas@email.com').should('be.visible');
    });

    it('exibe documentos recentes do paciente', () => {
      cy.contains('h2', 'Documentos recentes').scrollIntoView().should('be.visible');
      cy.contains('Laudo').scrollIntoView().should('exist');
    });

    it('exibe alerta de consultas aguardando confirmação de presença', () => {
      cy.contains('Confirme sua presença').should('be.visible');
      cy.contains('Você tem 1 consulta(s) aguardando confirmação de presença.').should('be.visible');
      cy.contains('a', 'Minhas consultas').should('have.attr', 'href', '/panel/my-appointments');
    });
  });

});
