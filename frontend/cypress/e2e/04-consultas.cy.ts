/// <reference types="cypress" />

describe('04 — Consultas (Agendamento, Confirmação, Cancelamento)', () => {

  it('Paciente — lista de consultas carrega', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    cy.contains('Minhas Consultas', { timeout: 10000 }).should('be.visible');
  });

  it('Paciente — consultas do Lucas aparecem na lista', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    // Lucas tem consultas com Dra. Ana e Dr. Carlos no DataInitializer
    cy.contains('Dra. Ana Souza', { timeout: 10000 }).should('exist');
  });

  it('Paciente — formulário de agendamento aparece ao clicar', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    cy.contains('Agendar consulta', { timeout: 10000 }).click();
    cy.contains('Nova consulta').should('be.visible');
    cy.get('select').should('exist'); // Select de profissionais
    cy.contains('Escolha o profissional').should('exist');
  });

  it('Paciente — consultas exibem labels de status', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-appointments');
    // Pelo DataInitializer, há consultas com status variados
    cy.get('[class*="rounded-full"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Profissional — agenda completa carrega', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    cy.contains('Lucas Silva', { timeout: 10000 }).should('exist');
  });

  it('Profissional — agenda exibe consultas de hoje', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/professional-appointments');
    // Existem consultas de hoje no DataInitializer (c6, c7)
    cy.get('[class*="rounded-full"]', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Admin — agenda geral carrega', () => {
    cy.login('admin@clinica.com', 'admin');
    cy.visit('/panel/appointments');
    // Admin vê consultas de todos
    cy.get('table, [class*="border"]', { timeout: 10000 }).should('exist');
  });

  it('Paciente — deve abrir modal de cancelamento e exigir justificativa', () => {
    cy.login('lucas@email.com', '123456');
    cy.intercept('GET', '**/consultas/minhas').as('getMinhas');
    cy.visit('/panel/my-appointments');
    cy.wait('@getMinhas');
    
    // Procura por uma consulta que pode ser cancelada usando testid
    cy.get('[data-testid="cancelar-button"]').first().click();
    
    cy.contains('Cancelar Consulta').should('be.visible');
    cy.contains('Confirmar Cancelamento').should('be.disabled');
    
    // Preencher justificativa curta (inválido)
    cy.get('textarea').type('Curta');
    cy.contains('Confirmar Cancelamento').should('be.disabled');
    
    // Justificativa válida
    cy.get('textarea').clear().type('Justificativa com mais de 10 caracteres');
    cy.contains('Confirmar Cancelamento').should('not.be.disabled');
    
    cy.contains('Voltar').click();
    cy.contains('Cancelar Consulta').should('not.exist');
  });

  it('Paciente — deve abrir modal de reagendamento', () => {
    cy.login('lucas@email.com', '123456');
    cy.intercept('GET', '**/consultas/minhas').as('getMinhasReagendar');
    cy.visit('/panel/my-appointments');
    cy.wait('@getMinhasReagendar');
    
    cy.get('[data-testid="reagendar-button"]').first().click();
    cy.contains('Reagendar Consulta').should('be.visible');
    cy.get('select').should('exist'); // Select de datas
    
    cy.contains('Voltar').click();
    cy.contains('Reagendar Consulta').should('not.exist');
  });

  it('Paciente — deve exibir aviso se consulta estiver a menos de 24h', () => {
    cy.login('lucas@email.com', '123456');
    cy.intercept('GET', '**/consultas/minhas').as('getMinhasAviso');
    cy.visit('/panel/my-appointments');
    cy.wait('@getMinhasAviso');
    
    // No DataInitializer, algumas consultas são de hoje (menos de 24h)
    // Elas não devem ter os botões "Cancelar" ou "Reagendar"
    // E devem exibir o aviso
    cy.contains('⚠️ Ação tardia:').should('exist');
  });
});
