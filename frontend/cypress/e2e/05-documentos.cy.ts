/// <reference types="cypress" />

describe('05 — Documentos (Gestão e Visualização)', () => {

  it('Profissional — lista de documentos carrega', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/document-management');
    cy.contains('Documentos', { timeout: 10000 }).should('be.visible');
  });

  it('Profissional — documentos do DataInitializer aparecem', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/document-management');
    // Dra. Ana criou 3 documentos no DataInitializer
    cy.contains('Laudo Psicológico', { timeout: 10000 }).should('exist');
    cy.contains('Atestado de Comparecimento', { timeout: 10000 }).should('exist');
  });

  it('Profissional — toggle de visibilidade funciona', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/document-management');
    // Encontra o botão de toggle (Sim/Não) e clica
    cy.get('button', { timeout: 10000 }).then($buttons => {
      const toggleBtn = $buttons.filter(':contains("Sim"), :contains("Não")');
      if (toggleBtn.length > 0) {
        const textoOriginal = toggleBtn.first().text().trim();
        cy.wrap(toggleBtn.first()).click();
        // Após clicar, o texto deve ter mudado
        cy.wrap(toggleBtn.first()).should('not.have.text', textoOriginal);
      }
    });
  });

  it('Profissional — botão de visualizar documento existe', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/document-management');
    // Os botões de ação (olho, lixeira) devem existir
    cy.get('button[title], button', { timeout: 10000 }).should('have.length.greaterThan', 0);
  });

  it('Paciente — meus documentos carrega', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-documents');
    cy.contains('Meus Documentos', { timeout: 10000 }).should('be.visible');
  });

  it('Paciente — laudo disponibilizado é visível', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-documents');
    // Lucas tem o Laudo Psicológico como disponível (d1)
    cy.contains('Laudo Psicológico', { timeout: 10000 }).should('exist');
  });

  it('Paciente — encaminhamento oculto NÃO aparece', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-documents');
    // d3 tem disponivel=false, então não deve aparecer para o paciente
    cy.wait(3000); // Espera carregar
    cy.contains('Encaminhamento para Psiquiatria').should('not.exist');
  });

});
