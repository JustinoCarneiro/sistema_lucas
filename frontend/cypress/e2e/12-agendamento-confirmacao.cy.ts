/// <reference types="cypress" />

describe('12 — Agendamento Guiado e Fluxo de Confirmação', () => {

  const profissionaisDisp = [
    { id: 1, name: 'Dra. Ana Souza', specialty: 'Psicologia Clínica' },
    { id: 2, name: 'Dr. Carlos Menezes', specialty: 'Psiquiatria' }
  ];

  // Próxima segunda-feira a partir de hoje (sempre futura)
  function proximaSegunda(): string {
    const d = new Date();
    const offset = ((1 - d.getDay() + 7) % 7) || 7;
    d.setDate(d.getDate() + offset);
    return d.toISOString().split('T')[0];
  }

  const dataAlvo = proximaSegunda();

  const slotsDisponiveis = [
    { startTime: '09:00:00', endTime: '10:00:00' },
    { startTime: '10:00:00', endTime: '11:00:00' },
    { startTime: '14:00:00', endTime: '15:00:00' }
  ];

  context('Paciente — Agendamento Guiado (fluxo de 3 etapas)', () => {
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
        body: [dataAlvo]
      }).as('getAvailableDates');
      cy.intercept('GET', '**/disponibilidade/*/slots*', {
        statusCode: 200,
        body: slotsDisponiveis
      }).as('getSlots');

      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhas');
    });

    it('Etapa 1: formulário exibe select de profissionais', () => {
      cy.contains('button', '+ Agendar consulta').click();
      cy.contains('h2', 'Nova consulta').should('be.visible');
      cy.contains('label', '1. Escolha o profissional').should('be.visible');
      cy.get('select[formControlName="professionalId"]')
        .find('option').should('have.length.at.least', 3);
    });

    it('Etapa 2: ao escolher profissional, working-days e select de datas aparecem', () => {
      cy.contains('button', '+ Agendar consulta').click();
      cy.get('select[formControlName="professionalId"]').select('1');
      cy.wait('@getAvailableDates');
      cy.contains('label', '2. Escolha a data').should('be.visible');
      cy.get('select[formControlName="date"]').should('exist');
    });

    it('Etapa 3: ao escolher data, slots aparecem como botões', () => {
      cy.contains('button', '+ Agendar consulta').click();
      cy.get('select[formControlName="professionalId"]').select('1');
      cy.wait('@getAvailableDates');
      cy.get('select[formControlName="date"] option').should('have.length.at.least', 2);
      cy.get('select[formControlName="date"]').find('option').eq(1).then(opt => {
        cy.get('select[formControlName="date"]').select(String(opt.val()));
      });
      cy.wait('@getSlots');
      cy.contains('label', /3\. Escolha o horário/).should('be.visible');
      cy.get('[data-testid="slot-button"]').should('have.length', 3);
      cy.get('[data-testid="slot-button"]').first().should('contain', '09:00');
    });

    it('exibe resumo quando slot é selecionado', () => {
      cy.contains('button', '+ Agendar consulta').click();
      cy.get('select[formControlName="professionalId"]').select('1');
      cy.wait('@getAvailableDates');
      cy.get('select[formControlName="date"] option').should('have.length.at.least', 2);
      cy.get('select[formControlName="date"]').find('option').eq(1).then(opt => {
        cy.get('select[formControlName="date"]').select(String(opt.val()));
      });
      cy.wait('@getSlots');
      cy.get('[data-testid="slot-button"]').first().click();
      cy.contains('p', 'Resumo do agendamento').should('be.visible');
      cy.contains('span.font-medium', 'Dra. Ana Souza').should('be.visible');
      cy.contains('button', 'Confirmar agendamento').should('be.visible');
    });

    it('Fluxo completo: POST /consultas e recarrega minhas com status AGUARDANDO_CONFIRMACAO', () => {
      cy.intercept('POST', '**/consultas', {
        statusCode: 201,
        body: 'Consulta agendada'
      }).as('agendarPost');

      // Após criar, retorna a consulta com status pendente de aprovação
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [
          {
            id: 100,
            patientId: 3,
            professionalName: 'Dra. Ana Souza',
            patientName: 'Lucas Silva',
            startTime: `${dataAlvo}T09:00:00`,
            reason: 'Teste Cypress — fluxo completo',
            cancelReason: null,
            status: 'AGUARDANDO_CONFIRMACAO',
            podeCancelar: true,
            atrasada: false
          }
        ]
      }).as('reloadMinhas');
      cy.contains('button', '+ Agendar consulta').click();
      cy.get('select[formControlName="professionalId"]').select('1');
      cy.wait('@getAvailableDates');
      cy.get('select[formControlName="date"] option').should('have.length.at.least', 2);
      cy.get('select[formControlName="date"]').find('option').eq(1).then(opt => {
        cy.get('select[formControlName="date"]').select(String(opt.val()));
      });
      cy.wait('@getSlots');
      cy.get('[data-testid="slot-button"]').first().click();
      cy.get('[data-testid="reason-input"]').type('Teste Cypress — fluxo completo');
      cy.contains('button', 'Confirmar agendamento').click();

      cy.wait('@agendarPost').then(({ request }) => {
        expect(request.body.professionalId).to.eq(1);
        expect(request.body.reason).to.eq('Teste Cypress — fluxo completo');
        expect(request.body.dateTime).to.match(/T09:00:00$/);
      });
      cy.wait('@reloadMinhas');

      cy.contains('[role="alert"]', /Consulta agendada/).should('be.visible');
      cy.contains('Pendente de aprovação').should('be.visible');
      cy.contains('Aguardando Confirmação').should('be.visible');
    });

    it('confirmar presença como paciente após profissional ter confirmado', () => {
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [
          {
            id: 50,
            patientId: 3,
            professionalName: 'Dra. Ana Souza',
            patientName: 'Lucas Silva',
            startTime: `${dataAlvo}T11:00:00`,
            reason: 'Sessão de acompanhamento',
            cancelReason: null,
            status: 'CONFIRMADA_PROFISSIONAL',
            podeCancelar: true,
            atrasada: false
          }
        ]
      }).as('getMinhasConfirmadaProf');

      cy.intercept('PATCH', '**/consultas/50/confirmar-paciente', {
        statusCode: 200,
        body: 'Presença confirmada'
      }).as('patchConfirmar');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhasConfirmadaProf');

      cy.contains('button', 'Confirmar presença').click();
      cy.wait('@patchConfirmar');
      cy.contains('[role="alert"]', /Presença confirmada/).should('be.visible');
    });
  });

  context('Paciente — Reagendamento', () => {
    beforeEach(() => {

      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [
          {
            id: 77,
            patientId: 3,
            professionalName: 'Dra. Ana Souza',
            patientName: 'Lucas Silva',
            startTime: `${dataAlvo}T09:00:00`,
            reason: 'Sessão',
            cancelReason: null,
            status: 'AGENDADA',
            podeCancelar: true,
            atrasada: false
          }
        ]
      }).as('getMinhasAgendada');
      cy.intercept('GET', '**/disponibilidade/profissionais-disponiveis', {
        statusCode: 200,
        body: profissionaisDisp
      }).as('getProfs');
      cy.intercept('GET', '**/disponibilidade/*/available-dates*', {
        statusCode: 200,
        body: [dataAlvo]
      }).as('getAvailableDates');
      cy.intercept('GET', '**/disponibilidade/*/slots*', {
        statusCode: 200,
        body: [{ startTime: '10:00:00', endTime: '11:00:00' }]
      }).as('getSlots');

      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-appointments');
      cy.wait('@getMinhasAgendada');
    });

    it('reagendamento sem justificativa → botão Confirmar Reagendamento desabilitado', () => {
      cy.get('[data-testid="reagendar-button"]').first().click();
      cy.wait('@getAvailableDates');
      
      cy.contains('h3', 'Reagendar Consulta').should('be.visible');

      // Seleciona a data (o profissional já é auto-selecionado)
      cy.get('select[formControlName="date"]').select(dataAlvo);
      cy.wait('@getSlots');

      // Seleciona o slot (neste caso, o mock tem apenas 1 slot "10:00:00")
      cy.contains('button', '10:00').click();

      cy.contains('button', /Confirmar Alteração/i).should('be.disabled');
    });

    it('lista mostra consulta com status CONCLUIDA com badge "Concluída"', () => {
      cy.intercept('GET', '**/consultas/minhas', {
        statusCode: 200,
        body: [
          {
            id: 88,
            patientId: 3,
            professionalName: 'Dra. Ana Souza',
            patientName: 'Lucas Silva',
            startTime: '2026-04-01T09:00:00',
            reason: 'Sessão concluída',
            cancelReason: null,
            status: 'CONCLUIDA',
            podeCancelar: false,
            atrasada: false
          }
        ]
      }).as('getConcluida');

      cy.visit('/panel/my-appointments');
      cy.wait('@getConcluida');
      cy.contains('Concluída').should('be.visible');
    });
  });

  context('Profissional — Aprovação / Recusa de pendente', () => {
    const consultasHoje: any[] = [];
    const todasConsultasComPendente = [
      {
        id: 100,
        patientName: 'Lucas Silva',
        professionalName: 'Dra. Ana Souza',
        startTime: `${dataAlvo}T09:00:00`,
        reason: 'Avaliação inicial',
        status: 'AGUARDANDO_CONFIRMACAO',
        podeCancelar: true,
        atrasada: false
      }
    ];

    beforeEach(() => {
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
        body: todasConsultasComPendente
      }).as('getTodas');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/professional-appointments');
      cy.wait('@getHoje');
    });

    it('exibe aba Próximas com consulta pendente e botões Aprovar/Recusar', () => {
      cy.contains('button', 'Próximas').click();
      cy.wait('@getTodas');

      cy.contains('Aguardando Confirmação').should('be.visible');
      cy.contains('Lucas Silva').should('be.visible');
      cy.contains('button', /Aprovar/).should('be.visible');
      cy.contains('button', /Recusar/).should('be.visible');
    });

    it('aprovar consulta envia PATCH /consultas/:id/aprovar', () => {
      cy.intercept('PATCH', '**/consultas/100/aprovar', {
        statusCode: 200,
        body: 'OK'
      }).as('aprovarReq');
      // Após aprovar, recarrega com status AGENDADA
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: [{ ...todasConsultasComPendente[0], status: 'AGENDADA' }]
      }).as('reloadTodas');

      cy.on('window:confirm', () => true);
      cy.contains('button', 'Próximas').click();
      cy.wait('@getTodas');

      cy.contains('button', /Aprovar/).click();
      cy.wait('@aprovarReq');
      cy.contains('[role="alert"]', /Agendamento confirmado/).should('be.visible');
    });

    it('recusar consulta abre prompt e envia PATCH /consultas/:id/recusar', () => {
      cy.intercept('PATCH', '**/consultas/100/recusar', {
        statusCode: 200,
        body: 'OK'
      }).as('recusarReq');
      cy.intercept('GET', '**/consultas/profissional/todas', {
        statusCode: 200,
        body: [{ ...todasConsultasComPendente[0], status: 'CANCELADA' }]
      }).as('reloadTodasRecusa');
      cy.window().then((win) => {
        cy.stub(win, 'prompt').returns('Agenda lotada neste horário.');
      });

      cy.contains('button', 'Próximas').click();
      cy.wait('@getTodas');

      cy.contains('button', /Recusar/).click();
      cy.wait('@recusarReq').then(({ request }) => {
        expect(request.body.justification).to.eq('Agenda lotada neste horário.');
      });
      cy.contains('[role="alert"]', /recusado com sucesso/).should('be.visible');
    });

    it('aba Hoje — confirma consulta com status AGENDADA via PATCH /confirmar-profissional', () => {
      cy.intercept('GET', '**/consultas/profissional/hoje', {
        statusCode: 200,
        body: [
          {
            id: 200,
            patientName: 'Lucas Silva',
            professionalName: 'Dra. Ana Souza',
            startTime: new Date().toISOString(),
            reason: 'Sessão',
            status: 'AGENDADA',
            podeCancelar: true,
            atrasada: false
          }
        ]
      }).as('getHojeComAgendada');
      cy.intercept('PATCH', '**/consultas/200/confirmar-profissional', {
        statusCode: 200,
        body: 'Confirmada'
      }).as('confirmarProf');

      cy.on('window:confirm', () => true);
      cy.visit('/panel/professional-appointments');
      cy.wait('@getHojeComAgendada');

      cy.contains('button', 'Confirmar').click();
      cy.wait('@confirmarProf');
    });
  });

});
