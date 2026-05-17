/// <reference types="cypress" />

describe('13 — Penalidades por Cancelamento Tardio', () => {

  const patientEmail = 'lucas@email.com';
  const patientPass = '123456';
  const adminEmail = 'admin@clinica.com';
  const adminPass = 'admin';

  it('Fluxo Completo: Cancelamento Tardio → Bloqueio → Desbloqueio Admin', () => {
    
    // 1. Paciente — Realiza cancelamento tardio
    cy.login(patientEmail, patientPass);
    cy.intercept('GET', '**/consultas/minhas').as('getMinhas');
    cy.visit('/panel/my-appointments');
    cy.wait('@getMinhas');
    
    // Procura pela consulta que tem o aviso de ação tardia
    cy.contains('⚠️ Ação tardia', { timeout: 15000 })
      .parents('.bg-white')
      .find('[data-testid="cancelar-button"]')
      .click();

    // Verifica aviso no modal
    cy.contains('AVISO DE PENALIDADE').should('be.visible');
    cy.contains('bloqueado para novos agendamentos por 14 dias').should('be.visible');

    // Justificativa e Confirmação
    cy.get('textarea').type('Cancelamento tardio para teste de penalidade');
    cy.contains('Confirmar Cancelamento').click();
    
    // Verifica se status mudou para Cancelada
    cy.contains('Cancelada', { timeout: 10000 }).should('exist');

    // 2. Paciente — Tenta agendar nova consulta e deve ser bloqueado
    cy.contains('Agendar consulta').click();
    cy.get('select').first().select(1); // Seleciona qualquer profissional
    cy.get('select#date', { timeout: 10000 }).should('exist').select(2); // Seleciona uma data futura
    cy.get('[data-testid="slot-button"]', { timeout: 10000 }).first().click({ force: true });
    
    // Deve exibir mensagem de erro de bloqueio vinda do backend (alert nativo)
    let blockedAlertCalled = false;
    cy.on('window:alert', (text) => {
      if (text.includes('Você está temporariamente bloqueado para novos agendamentos')) {
        blockedAlertCalled = true;
      }
    });
    cy.intercept('POST', '**/consultas').as('agendarRequest');
    cy.get('button').contains('Confirmar agendamento').click();
    cy.wait('@agendarRequest');
    cy.wrap(null).should(() => {
      expect(blockedAlertCalled).to.be.true;
    });

    // 3. Admin — Desbloqueia o paciente
    cy.login(adminEmail, adminPass);
    cy.visit('/panel/patients');
    
    // Localiza Lucas Silva que deve estar com a tag "Bloqueado"
    let unlockedAlertCalled = false;
    cy.on('window:alert', (text) => {
      if (text.includes('Paciente desbloqueado com sucesso!')) {
        unlockedAlertCalled = true;
      }
    });

    cy.contains('Lucas Silva')
      .parents('tr')
      .within(() => {
        cy.contains('Bloqueado').should('exist');
        cy.contains('Desbloquear').click();
      });

    cy.wrap(null).should(() => {
      expect(unlockedAlertCalled).to.be.true;
    });

    cy.contains('Lucas Silva')
      .parents('tr')
      .should('not.contain', 'Bloqueado');

    // 4. Paciente — Tenta agendar novamente e agora DEVE conseguir
    cy.login(patientEmail, patientPass);
    cy.intercept("GET", "**/consultas/minhas").as("getMinhasPosDesbloqueio");
    cy.visit("/panel/my-appointments");
    cy.wait("@getMinhasPosDesbloqueio");
    
    cy.contains('Agendar consulta').click();
    cy.get('select').first().select(1);
    cy.get('select#date').select(2); // Seleciona uma data futura
    cy.get('[data-testid="slot-button"]').first().click({ force: true });
    
    cy.get('[data-testid="reason-input"]').type('Teste após desbloqueio');

    let scheduleSuccessAlertCalled = false;
    cy.on('window:alert', (text) => {
      if (text.includes('Consulta agendada com sucesso!')) {
        scheduleSuccessAlertCalled = true;
      }
    });
    cy.get('button').contains('Confirmar agendamento').click();
    cy.wrap(null).should(() => {
      expect(scheduleSuccessAlertCalled).to.be.true;
    });

    cy.contains('Agendada').should('exist');
  });

  it('Paciente — Reagendamento tardio também deve aplicar penalidade', () => {
    cy.login(patientEmail, patientPass);
    cy.visit('/panel/my-appointments');

    // Procura por outra consulta tardia (ou a mesma se houver)
    cy.contains('⚠️ Ação tardia', { timeout: 15000 })
      .parents('.bg-white')
      .find('[data-testid="reagendar-button"]')
      .click();

    cy.contains('AVISO DE PENALIDADE').should('be.visible');
    cy.contains('bloqueado para novos agendamentos por 14 dias').should('be.visible');
  });

});
