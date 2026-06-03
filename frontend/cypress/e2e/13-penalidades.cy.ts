/// <reference types="cypress" />

describe('13 — Penalidades por Cancelamento/Reagendamento Tardio', () => {

  const profissionaisDisp = [
    { id: 1, name: 'Dra. Ana Souza', specialty: 'Psicologia Clínica' }
  ];

  function amanha(): string {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  context('Paciente — Cancelamento Tardio', () => {
    const consultaAtrasada = {
      id: 50,
      patientId: 3,
      professionalName: 'Dra. Ana Souza',
      patientName: 'Lucas Silva',
      startTime: `${amanha()}T09:00:00`,
      reason: 'Sessão semanal',
      cancelReason: null,
      status: 'CONFIRMADA',
      podeCancelar: true,
      atrasada: true
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [consultaAtrasada]
      }).as('getMinhas');
      cy.intercept('GET', '**/disponibilidade/profissionais-disponiveis', {
        statusCode: 200,
        body: profissionaisDisp
      }).as('getProfs');

      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhas');
    });

    it('lista exibe aviso "⚠️ Ação tardia" para consulta com atrasada=true', () => {
      cy.contains('⚠️ Ação tardia:').should('be.visible');
      cy.contains('Cancelar/Reagendar aplicará penalidade de 2 semanas').should('be.visible');
    });

    it('modal de cancelamento exibe "AVISO DE PENALIDADE" + texto de 15 dias', () => {
      cy.get('[data-testid="cancelar-button"]').click();
      cy.contains('h3', 'Cancelar Consulta').should('be.visible');
      cy.contains('⚠️ AVISO DE PENALIDADE').should('be.visible');
      cy.contains('faltam menos de 24h para a consulta').should('be.visible');
      cy.contains('bloqueado para novos agendamentos por 15 dias').should('be.visible');
    });

    it('modal de reagendamento também exibe o aviso de penalidade', () => {
      cy.intercept('GET', '**/disponibilidade/*/available-dates*', {
        statusCode: 200,
        body: [amanha()]
      }).as('getAvailableDates');

      cy.get('[data-testid="reagendar-button"]').click();
      cy.wait('@getAvailableDates');
      cy.contains('h3', 'Reagendar Consulta').should('be.visible');
      cy.contains('⚠️ AVISO DE PENALIDADE').should('be.visible');
      cy.contains('faltam menos de 24h para a consulta original').should('be.visible');
      cy.contains('bloqueado para novos agendamentos por 15 dias').should('be.visible');
    });

    it('confirma cancelamento tardio com justificativa válida → POST /cancelar', () => {
      cy.intercept('POST', '**/consultas/50/cancelar', {
        statusCode: 200,
        body: { message: 'Consulta cancelada com penalidade aplicada.' }
      }).as('postCancel');

      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [{ ...consultaAtrasada, status: 'CANCELADA', podeCancelar: false }]
      }).as('reloadMinhas');

      cy.get('[data-testid="cancelar-button"]').click();
      cy.get('textarea[formControlName="justification"]')
        .type('Imprevisto pessoal de última hora, não consigo comparecer.');
      cy.contains('button', 'Confirmar Cancelamento').click();

      cy.wait('@postCancel').then(({ request }) => {
        expect(request.body.justification).to.contain('Imprevisto pessoal');
      });
      cy.wait('@reloadMinhas');
      cy.contains('Cancelada').should('be.visible');
    });
  });

  context('Paciente Bloqueado — Tentativa de Agendamento', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/consultas/minhas', { statusCode: 200, body: [] }).as('getMinhas');
      cy.intercept('GET', '**/disponibilidade/profissionais-disponiveis', {
        statusCode: 200,
        body: profissionaisDisp
      }).as('getProfs');
      cy.intercept('GET', '**/disponibilidade/*/available-dates*', {
        statusCode: 200,
        body: [amanha()]
      }).as('getAvailableDates');
      cy.intercept('GET', '**/disponibilidade/*/slots*', {
        statusCode: 200,
        body: [{ startTime: '09:00:00', endTime: '10:00:00' }]
      }).as('getSlots');

      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhas');
    });

    it('backend retorna 403 com mensagem de bloqueio → exibe alert', () => {
      cy.intercept('POST', '**/consultas', {
        statusCode: 403,
        body: { message: 'Você está temporariamente bloqueado para novos agendamentos até 2026-06-03.' }
      }).as('postBlocked');
      cy.contains('button', '+ Agendar consulta').click();
      cy.get('select[formControlName="professionalId"]').select('1');
      cy.wait('@getAvailableDates');
      cy.get('select[formControlName="date"] option').should('have.length.at.least', 2);
      cy.get('select[formControlName="date"]').find('option').eq(1).then(opt => {
        cy.get('select[formControlName="date"]').select(String(opt.val()));
      });
      cy.wait('@getSlots');
      cy.get('[data-testid="slot-button"]').first().click();
      cy.get('[data-testid="reason-input"]').type('Tentativa de agendamento');
      cy.contains('button', 'Confirmar agendamento').click();

      cy.wait('@postBlocked');
      cy.contains('[role="alert"]', /temporariamente bloqueado/).should('be.visible');
    });
  });

  context('Admin — Visualização e Desbloqueio de Paciente', () => {
    const blockedUntil = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString();
    const patientsBloqueado = [
      {
        id: 3,
        name: 'Lucas Silva',
        email: 'lucas@email.com',
        cpf: '111.222.333-44',
        phone: '(11) 88888-9999',
        blockedUntil,
        infractionCount: 2,
        receivedFirstWarning: true
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 0, totalPacientes: 1, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getAdminDash');
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: patientsBloqueado
      }).as('getPatients');

      cy.login('admin@clinica.com', 'admin');
      cy.visit('/panel/patients');
      cy.wait('@getPatients');
    });

    it('paciente bloqueado aparece com tag "Bloqueado" e botão "Desbloquear"', () => {
      cy.contains('tr', 'Lucas Silva').within(() => {
        cy.contains('Bloqueado').should('be.visible');
        cy.contains('button', 'Desbloquear').should('be.visible');
      });
    });

    it('filtro "Bloqueados Temporariamente" lista apenas pacientes bloqueados', () => {
      cy.contains('button', 'Bloqueados Temporariamente').click();
      cy.contains('tr', 'Lucas Silva').should('be.visible');
    });

    it('filtro "Com Histórico de Ausências" lista paciente com infrações', () => {
      cy.contains('button', 'Com Histórico de Ausências').click();
      cy.contains('tr', 'Lucas Silva').should('be.visible');
    });

    it('desbloqueia paciente via PATCH /patients/:id/desbloquear', () => {
      cy.intercept('PATCH', 'http://localhost:8081/patients/3/desbloquear', {
        statusCode: 200,
        body: 'Paciente desbloqueado.'
      }).as('patchDes');
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: [{ ...patientsBloqueado[0], blockedUntil: null }]
      }).as('reloadPatients');
      cy.on('window:confirm', () => true);

      cy.contains('tr', 'Lucas Silva').contains('button', 'Desbloquear').click();
      cy.wait('@patchDes');
      cy.wait('@reloadPatients');

      cy.contains('[role="alert"]', /desbloqueado com sucesso/).should('be.visible');
      cy.contains('tr', 'Lucas Silva').within(() => {
        cy.contains('Bloqueado').should('not.exist');
        cy.contains('button', 'Desbloquear').should('not.exist');
      });
    });
  });

  context('Profissional — Marcar Falta (penalidade progressiva)', () => {
    const consultaHoje = {
      id: 60,
      patientName: 'Lucas Silva',
      professionalName: 'Dra. Ana Souza',
      startTime: new Date().toISOString(),
      reason: 'Sessão semanal',
      status: 'CONFIRMADA',
      podeCancelar: true,
      atrasada: false
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: [consultaHoje]
      }).as('getHoje');
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: []
      }).as('getTodas');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/professional-appointments');
      cy.wait('@getHoje');
    });

    it('profissional marca falta → PATCH /falta → alert de confirmação e lista recarrega', () => {
      cy.intercept('PATCH', '**/consultas/60/falta', {
        statusCode: 200,
        body: 'OK'
      }).as('patchFalta');
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: [{ ...consultaHoje, status: 'FALTA' }]
      }).as('reloadHoje');
      cy.contains('Lucas Silva').should('be.visible');
      cy.contains('button', /Paciente faltou/i).click();

      cy.wait('@patchFalta');
      cy.contains('[role="alert"]', /faltante|falta/i).should('be.visible');
    });
  });

  context('Backend — endpoint /patients/:id/desbloquear protegido', () => {
    it('Paciente NÃO pode chamar /desbloquear (apenas Admin) — retorna 403', () => {
      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiUrl')}/auth/login`,
        body: { email: 'lucas@email.com', password: '123456' }
      }).then(() => {
        cy.request({
          method: 'PATCH',
          url: `${Cypress.env('apiUrl')}/patients/4/desbloquear`,
          failOnStatusCode: false
        }).then((resp) => {
          expect(resp.status).to.eq(403);
        });
      });
    });
  });

});
