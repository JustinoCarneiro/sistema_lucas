/// <reference types="cypress" />

describe('03 — Perfil e Senha', () => {

  it('Paciente — dados carregados corretamente', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Meu Perfil', { timeout: 10000 }).should('be.visible');
    // Nome do Lucas deve estar preenchido
    cy.get('input').first().should('have.value', 'Lucas Silva');
  });

  it('Paciente — editar nome e salvar', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Meu Perfil', { timeout: 10000 }).should('be.visible');

    // Limpa e digita novo nome
    cy.get('input').first().clear().type('Lucas Silva Editado');
    cy.contains('Salvar alterações').click();
    cy.on('window:alert', (text) => {
      expect(text).to.contain('Perfil atualizado');
    });

    // Recarrega e verifica persistência
    cy.visit('/panel/my-profile');
    cy.get('input', { timeout: 10000 }).first().should('have.value', 'Lucas Silva Editado');

    // Restaura o nome original
    cy.get('input').first().clear().type('Lucas Silva');
    cy.contains('Salvar alterações').click();
  });

  it('Paciente — modal de alteração de senha abre e fecha', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Alterar minha senha', { timeout: 10000 }).click();
    cy.contains('Alterar Senha').should('be.visible');
    cy.contains('Cancelar').click();
    cy.contains('Alterar Senha').should('not.exist');
  });

  it('Paciente — senhas não coincidem mostra erro', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Alterar minha senha', { timeout: 10000 }).click();
    cy.get('input[type="password"]').first().type('novaSenha123');
    cy.get('input[type="password"]').last().type('outraSenha456');
    cy.contains('As senhas não coincidem').should('be.visible');
  });

  it('Profissional — campos exclusivos editáveis', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Meu Perfil', { timeout: 10000 }).should('be.visible');

    // Tipo de Registro deve ser um select com CRP
    cy.get('select').first().should('contain', 'CRP');
    // Número do registro pode ser editável
    cy.get('input').eq(3).should('not.have.value', '');
    // Especialidade
    cy.contains('Especialidade').should('be.visible');
  });

  it('Profissional — campos compartilhados visíveis', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('WhatsApp', { timeout: 10000 }).should('be.visible');
    cy.contains('Data de Nascimento').should('be.visible');
    cy.contains('Gênero').should('be.visible');
    cy.contains('Endereço completo').should('be.visible');
  });

  it('Paciente — botão excluir conta visível', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Excluir minha conta', { timeout: 10000 }).scrollIntoView().should('be.visible');
  });

  it('Profissional — botão excluir conta NÃO visível', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/my-profile');
    cy.contains('Meu Perfil', { timeout: 10000 }).should('be.visible');
    cy.contains('Excluir minha conta').should('not.exist');
  });

});
