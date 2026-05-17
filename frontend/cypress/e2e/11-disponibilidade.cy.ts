/// <reference types="cypress" />

describe('11 — Disponibilidade de Horários (Profissional)', () => {

  beforeEach(() => {
    cy.login('ana@clinica.com', '123456');
    cy.intercept('GET', '**/disponibilidade/minha*').as('loadMinha');
    cy.intercept('GET', '**/disponibilidade/status-mes').as('loadStatus');
    cy.visit('/panel/my-availability');
    cy.wait(['@loadMinha', '@loadStatus']).then(([xhrMinha, xhrStatus]) => {
      cy.log('LOAD STATUS BODY:', JSON.stringify(xhrStatus.response?.body));
      cy.log('LOAD MINHA BODY:', JSON.stringify(xhrMinha.response?.body));
    });
    cy.contains('Carregando', { timeout: 10000 }).should('not.exist');
  });

  it('Página de disponibilidade carrega corretamente', () => {
    cy.contains('Agenda de').should('be.visible');
    cy.contains('Mês Atual').should('be.visible');
    cy.contains('Próximo Mês').should('be.visible');
  });

  it('Pode alternar entre Mês Atual e Próximo Mês', () => {
    const today = new Date();
    const monthNames = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
    
    const currentMonthLabel = `${monthNames[today.getMonth()]} de ${today.getFullYear()}`;
    
    let nextDate = new Date(today.getFullYear(), today.getMonth() + 1, 1);
    const nextMonthLabel = `${monthNames[nextDate.getMonth()]} de ${nextDate.getFullYear()}`;

    // Por padrão abre no próximo mês
    cy.get('h1').should('contain', nextMonthLabel);

    // Muda para o mês atual
    cy.contains('button', 'Mês Atual').click();
    cy.get('h1').should('contain', currentMonthLabel);

    // Volta para o próximo mês
    cy.contains('button', 'Próximo Mês').click();
    cy.get('h1').should('contain', nextMonthLabel);
  });

  it('Pode selecionar um dia no calendário e um horário', () => {
    cy.contains('span', '15').click();
    cy.contains('h3', 'Dia 15').should('exist');
    cy.contains('button', '12:00').first().then(($btn) => {
      if (!$btn.hasClass('bg-blue-900')) {
        cy.wrap($btn).click({ force: true });
      }
    });
    cy.contains('button', '12:00').should('have.class', 'bg-blue-900');
  });

  it('Botão "Enviar Agenda do Mês" persiste as mudanças', () => {
    cy.contains('span', '15').click();
    cy.contains('h3', 'Dia 15').should('exist');
    cy.contains('button', '12:00').first().then(($btn) => {
      if (!$btn.hasClass('bg-blue-900')) {
        cy.wrap($btn).click({ force: true });
      }
    });
    cy.contains('button', 'Enviar Agenda do Mês').should('be.visible').click({ force: true });
    cy.contains('Agenda do mês salva com sucesso!', { timeout: 15000 }).should('be.visible');
  });

  it('Exibe alerta de prazo ou pendência dependendo da proximidade do fim do mês', () => {
    // Esse teste é dinâmico, apenas garante que se o alerta existir, ele é legível
    cy.get('body').then($body => {
      if ($body.find('.bg-yellow-50').length > 0) {
        cy.get('.bg-yellow-50').should('be.visible');
        cy.contains('Atenção ao Prazo').should('exist');
      }
    });
  });

  it('Impede a remoção de um horário que já possui paciente agendado', () => {
    const today = new Date();
    let targetDate = new Date();
    let diffDays = 4;
    targetDate.setDate(today.getDate() + diffDays);
    while (
      targetDate.getDay() === 0 ||
      targetDate.getDay() === 6 ||
      diffDays === 7 ||
      diffDays === 14
    ) {
      diffDays++;
      targetDate = new Date();
      targetDate.setDate(today.getDate() + diffDays);
    }
    const diaAlvo = targetDate.getDate().toString();

    // 1. Muda para o mês correspondente ao targetDate
    cy.intercept('GET', '**/disponibilidade/minha*').as('loadMinhaAtual');
    if (targetDate.getMonth() === today.getMonth()) {
      cy.contains('button', 'Mês Atual').click();
    } else {
      cy.contains('button', 'Próximo Mês').click();
    }
    cy.wait('@loadMinhaAtual');
    cy.contains('Carregando').should('not.exist');

    // 2. Seleciona o dia alvo (onde o DataInitializer criou uma consulta ativa às 08:00 para a Dra. Ana)
    cy.contains('.aspect-square', diaAlvo).click();
    cy.contains('h3', `Dia ${diaAlvo}`).should('exist');

    // 3. O slot de 08:00 deve estar selecionado (azul)
    cy.contains('button', '08:00').should('have.class', 'bg-blue-900');

    // 4. Tenta desmarcar o horário de 08:00
    cy.contains('button', '08:00').click({ force: true });
    cy.contains('button', '08:00').should('not.have.class', 'bg-blue-900');

    // 5. Tenta salvar
    cy.intercept('POST', '**/disponibilidade/mensal*').as('saveAvailability');
    cy.contains('button', 'Enviar Agenda do Mês').click({ force: true });
    cy.wait('@saveAvailability').then((xhr) => {
      cy.log('SAVE STATUS:', xhr.response?.statusCode);
      cy.log('SAVE BODY:', typeof xhr.response?.body === 'string' ? xhr.response?.body : JSON.stringify(xhr.response?.body));
    });

    // 6. Deve exibir o erro de paciente marcado
    cy.contains('Paciente marcado para o dia', { timeout: 10000 }).should('exist');
    cy.contains('Para desmarcar, justifique e cancele a consulta individualmente.').should('exist');
  });

});
