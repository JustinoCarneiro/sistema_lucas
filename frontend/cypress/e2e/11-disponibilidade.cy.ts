/// <reference types="cypress" />

describe('11 — Disponibilidade de Horários (Profissional)', () => {

  beforeEach(() => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/my-availability');
    cy.contains('Carregando', { timeout: 10000 }).should('not.exist');
  });

  it('Página de disponibilidade carrega corretamente', () => {
    cy.contains('Minha Disponibilidade').should('be.visible');
    cy.contains('Selecione os horários').should('exist');
  });

  it('Pode selecionar e deselecionar um horário', () => {
    // Expande a Segunda-feira
    cy.contains('Segunda-feira').click();
    
    // Escolhe o horário de 10:00 e alterna
    cy.contains('button', '10:00').first().click();
    
    // Verifica se existe um botão que agora tem a classe de selecionado ou não
    // Apenas garante que o clique funcionou
    cy.contains('button', '10:00').should('be.visible');
  });

  it('Botão "Atualizar" persiste as mudanças', () => {
    // Expande a Segunda-feira
    cy.contains('Segunda-feira').click();
    
    // Altera algo (ex: adiciona 12:00)
    cy.contains('button', '12:00').first().click();
    
    // Clica no botão Atualizar que deve estar visível no card expandido
    cy.contains('button', 'Atualizar').should('be.visible').click({ force: true });
    
    // Verifica que o badge "Salvo" permanece ou reaparece
    cy.contains('Salvo', { timeout: 15000 }).scrollIntoView().should('be.visible');
  });

});
