/// <reference types="cypress" />

describe('11 — Disponibilidade de Horários (Profissional)', () => {

  const monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  function monthLabel(offset: number): string {
    const today = new Date();
    const d = new Date(today.getFullYear(), today.getMonth() + offset, 1);
    return `${monthNames[d.getMonth()]} de ${d.getFullYear()}`;
  }

  function monthStr(offset: number): string {
    const today = new Date();
    const d = new Date(today.getFullYear(), today.getMonth() + offset, 1);
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    return `${d.getFullYear()}-${mm}`;
  }

  context('Renderização do calendário', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 20, bloqueado: false }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: []
      }).as('getMinha');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');
    });

    it('carrega título e abre por padrão no Próximo Mês', () => {
      cy.contains('h1', monthLabel(1)).should('be.visible');
      cy.contains('button', 'Próximo Mês').should('have.class', 'bg-white');
    });

    it('alterna para Mês Atual e volta para Próximo Mês', () => {
      cy.contains('button', 'Mês Atual').click();
      cy.contains('h1', monthLabel(0)).should('be.visible');

      cy.contains('button', 'Próximo Mês').click();
      cy.contains('h1', monthLabel(1)).should('be.visible');
    });

    it('exibe os 7 cabeçalhos de dias da semana', () => {
      ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'].forEach(d => {
        cy.contains(d).should('be.visible');
      });
    });

    it('mostra contador de "dias com horários" zerado quando não há slots', () => {
      cy.contains('span', '0 dias com horários').should('be.visible');
    });
  });

  context('Seleção de dia e slots', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 20, bloqueado: false }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: []
      }).as('getMinha');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');
    });

    it('exibe instrução quando nenhum dia está selecionado', () => {
      cy.contains('Selecione um dia no calendário').should('be.visible');
    });

    it('seleciona um dia e abre o painel de horários', () => {
      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('h3', /^Dia \d+$/).should('be.visible');
      cy.contains('button', '08:00').should('be.visible');
      cy.contains('button', '18:00').should('be.visible');
    });

    it('toggle de slot pinta de azul quando ativo', () => {
      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('button', '10:00')
        .should('not.have.class', 'bg-blue-900')
        .click()
        .should('have.class', 'bg-blue-900');

      // Clicar de novo desmarca
      cy.contains('button', '10:00').click()
        .should('not.have.class', 'bg-blue-900');
    });

    it('seleção de slot atualiza a marca visual do dia no calendário', () => {
      cy.get('.aspect-square').filter(':not(.opacity-0)').first().as('dia');
      cy.get('@dia').click();
      cy.contains('button', '14:00').click();
      cy.get('@dia').find('.bg-blue-600').should('exist');
    });
  });

  context('Persistência (POST /disponibilidade/mensal)', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 20, bloqueado: false }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: []
      }).as('getMinha');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');
    });

    it('salva agenda com sucesso e exibe mensagem verde', () => {
      cy.intercept('POST', '**/disponibilidade/mensal*', {
        statusCode: 200,
        body: 'OK'
      }).as('saveMensal');

      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('button', '09:00').click();
      cy.contains('button', '10:00').click();

      cy.contains('button', 'Enviar Agenda do Mês').click();

      cy.wait('@saveMensal').then(({ request }) => {
        expect(request.url).to.match(/mes=\d{4}-\d{2}/);
        const dtos = request.body as Array<{ date: string; startTimes: string[] }>;
        expect(dtos).to.have.length(1);
        expect(dtos[0].startTimes).to.have.members(['09:00:00', '10:00:00']);
      });

      cy.contains('Agenda do mês salva com sucesso!').should('be.visible');
    });

    it('exibe erro quando o backend rejeita por paciente já agendado', () => {
      cy.intercept('POST', '**/disponibilidade/mensal*', {
        statusCode: 409,
        body: {
          message: 'Paciente marcado para o dia 15. Para desmarcar, justifique e cancele a consulta individualmente.'
        }
      }).as('saveFail');

      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('button', '11:00').click();
      cy.contains('button', 'Enviar Agenda do Mês').click();

      cy.wait('@saveFail');
      cy.contains('Paciente marcado para o dia 15').should('be.visible');
      cy.contains('Para desmarcar, justifique e cancele a consulta individualmente.')
        .should('be.visible');
    });
  });

  context('Estado bloqueado (prazo encerrado)', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 0, bloqueado: true }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: []
      }).as('getMinha');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');
    });

    it('exibe alerta amarelo "Atenção ao Prazo" quando bloqueado', () => {
      cy.contains('h3', 'Atenção ao Prazo').should('be.visible');
      cy.contains('O prazo para submissão encerrou').should('be.visible');
    });

    it('desabilita botão Enviar e mostra aviso ao selecionar dia', () => {
      cy.contains('button', 'Enviar Agenda do Mês').should('be.disabled');
      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('Bloqueado para alterações').should('be.visible');
    });
  });

  context('Mês Atual com prazo encerrado (offset=0 bloqueado)', () => {
    it('ao alternar para "Mês Atual" com status bloqueado, slots permanecem readonly', () => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 0, bloqueado: true }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: []
      }).as('getMinha');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');

      // Alternar para a aba Mês Atual (offset = 0)
      cy.contains('button', 'Mês Atual').click();
      cy.contains('h1', monthLabel(0)).should('be.visible');

      // Botão Enviar deve permanecer desabilitado
      cy.contains('button', 'Enviar Agenda do Mês').should('be.disabled');

      // Selecionar um dia exibe aviso de bloqueio
      cy.get('.aspect-square').filter(':not(.opacity-0)').first().click();
      cy.contains('Bloqueado para alterações').should('be.visible');
    });
  });

  context('Carregamento de slots pré-existentes', () => {
    it('preenche os dias com slots vindos do backend', () => {
      const mes = monthStr(1);
      const slots = [
        { date: `${mes}-05`, startTime: '09:00:00', endTime: '10:00:00' },
        { date: `${mes}-05`, startTime: '10:00:00', endTime: '11:00:00' },
        { date: `${mes}-15`, startTime: '14:00:00', endTime: '15:00:00' }
      ];

      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/disponibilidade/status-mes', {
        statusCode: 200,
        body: { diasRestantes: 25, bloqueado: false }
      }).as('getStatus');
      cy.intercept('GET', '**/disponibilidade/minha*', {
        statusCode: 200,
        body: slots
      }).as('getMinha');

      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-availability');
      cy.wait('@getStatus');
      cy.contains('Carregando').should('not.exist');

      cy.contains('span', '2 dias com horários').should('be.visible');

      // Dia 05: dois slots
      cy.contains('.aspect-square', '5').click();
      cy.contains('button', '09:00').should('have.class', 'bg-blue-900');
      cy.contains('button', '10:00').should('have.class', 'bg-blue-900');
      cy.contains('button', '11:00').should('not.have.class', 'bg-blue-900');
    });
  });

});
