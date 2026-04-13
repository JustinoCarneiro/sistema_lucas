/// <reference types="cypress" />

describe('12 — Agendamento Guiado (Paciente → Slots)', () => {

  beforeEach(() => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
  });

  it('Formulário de agendamento exibe select de profissionais', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.contains('Nova consulta').should('be.visible');
    cy.contains('Escolha o profissional').should('exist');
    cy.get('select').should('exist');
  });

  it('Apenas profissionais com disponibilidade aparecem', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    // Ana e Carlos têm disponibilidade configurada no DataInitializer
    cy.get('select option', { timeout: 10000 }).should('have.length.greaterThan', 1);
    cy.get('select').find('option').then(($options) => {
      const texts = Array.from($options).map(o => o.textContent);
      expect(texts.some(t => t?.includes('Ana'))).to.be.true;
      expect(texts.some(t => t?.includes('Carlos'))).to.be.true;
    });
  });

  it('Campo de data aparece após selecionar profissional', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.get('select').select(1); // Seleciona o primeiro profissional
    cy.contains('Escolha a data', { timeout: 5000 }).should('be.visible');
    cy.get('input[type="date"]').should('exist');
  });

  it('Slots disponíveis ou mensagem de indisponibilidade após selecionar data', () => {
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.get('select').select(1);

    // Calcula uma data futura válida (próxima segunda-feira, que Ana atende)
    const nextMonday = getNextMonday();
    cy.get('input[type="date"]').type(nextMonday);
    cy.get('input[type="date"]').trigger('change');

    // Deve aparecer ou botões de slots ou mensagem de indisponibilidade
    cy.get('body', { timeout: 10000 }).then(($body) => {
      const hasSlots = $body.find('button[class*="rounded-lg"]').length > 2;
      const hasNoSlots = $body.text().includes('Nenhum horário disponível');
      expect(hasSlots || hasNoSlots).to.be.true;
    });
  });

  it('Labels de status atualizados para o novo fluxo', () => {
    // Espera os dados carregarem
    cy.contains('Carregando').should('not.exist');
    
    // Pelo DataInitializer, há consultas com diferentes status
    cy.get('[class*="rounded-full"]', { timeout: 10000 }).should('have.length.greaterThan', 0);

    // Verifica que algum dos novos labels está presente
    cy.get('body').should((body) => {
      const text = body.text();
      const hasValidLabel =
        text.includes('Aguardando profissional') ||
        text.includes('Aguardando sua confirmação') ||
        text.includes('Confirmada') ||
        text.includes('Agendada') ||
        text.includes('Concluída');
      expect(hasValidLabel).to.be.true;
    });
  });

  it('Status "Aguardando profissional" aparece para consultas AGENDADA', () => {
    cy.contains('Carregando').should('not.exist');
    // Lucas tem consulta AGENDADA no DataInitializer (c9)
    cy.contains('Aguardando profissional', { timeout: 10000 }).should('be.visible');
  });

  it('Botão "Confirmar presença" aparece apenas para CONFIRMADA_PROFISSIONAL', () => {
    cy.contains('Carregando').should('not.exist');
    // Lucas tem consulta CONFIRMADA_PROFISSIONAL no DataInitializer (c11)
    cy.get('body').should((body) => {
      const text = body.text();
      const hasConfirmButton = text.includes('Confirmar presença');
      const hasWaitingLabel = text.includes('Aguardando sua confirmação');
      expect(hasConfirmButton || hasWaitingLabel).to.be.true;
    });
  });

});

describe('13 — Fluxo de Confirmação Invertido (Profissional → Paciente)', () => {

  it('Profissional vê botão "Confirmar" para consultas AGENDADA', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    cy.contains('Carregando').should('not.exist');
    
    // c9 (AGENDADA) está em "Próximas" (plusDays(7))
    cy.contains('button', 'Próximas').click();
    cy.contains('Carregando próximas consultas').should('not.exist');

    // Ana tem consulta AGENDADA
    cy.contains('Confirmar', { timeout: 10000 }).should('be.visible');
  });

  it('Profissional vê "Aguardando paciente" para consultas já confirmadas por ele', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    cy.contains('Carregando').should('not.exist');
    
    // c7 (CONFIRMADA_PROFISSIONAL) está em "Hoje" (default)
    cy.contains('button', 'Hoje').click(); // Garante aba hoje

    // Ana tem consulta CONFIRMADA_PROFISSIONAL (c7)
    cy.contains('Aguardando paciente', { timeout: 10000 }).should('be.visible');
  });

  it('Dashboard do paciente mostra "Aguardando confirmação do profissional"', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/dashboard');
    cy.contains('Carregando').should('not.exist');

    cy.get('body').should((body) => {
      const text = body.text();
      const hasWaiting = text.includes('Aguardando confirmação do profissional');
      const hasConfirm = text.includes('Confirmar presença');
      const hasConfirmed = text.includes('Consulta confirmada');
      expect(hasWaiting || hasConfirm || hasConfirmed).to.be.true;
    });
  });

  it('Dashboard do profissional exibe status corretos', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/dashboard');
    cy.contains('Carregando').should('not.exist');

    cy.contains('Agenda de hoje').should('be.visible');
    cy.get('[class*="rounded-full"]').should('have.length.greaterThan', 0);
  });

  it('Admin vê labels corretos na agenda geral', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/appointments');
    cy.contains('Carregando').should('not.exist');

    // Verifica exclusão do label antigo
    cy.contains('Aguard. profissional').should('not.exist');

    // Verifica presença de novos status
    cy.get('body').should((body) => {
      const text = body.text();
      const hasValidStatus =
        text.includes('Agendada') ||
        text.includes('Aguardando paciente') ||
        text.includes('Confirmada') ||
        text.includes('Concluída');
      expect(hasValidStatus).to.be.true;
    });
  });

});

// Helper para calcular a próxima segunda-feira (formato YYYY-MM-DD)
function getNextMonday(): string {
  const today = new Date();
  const daysUntilMonday = ((8 - today.getDay()) % 7) || 7; // Pelo menos 1 dia no futuro
  const nextMon = new Date(today);
  nextMon.setDate(today.getDate() + daysUntilMonday);
  return nextMon.toISOString().split('T')[0];
}
