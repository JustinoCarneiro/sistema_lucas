/// <reference types="cypress" />

describe('10 — Registro de Pacientes', () => {

  beforeEach(() => {
    cy.visit('/register');
  });

  it('Deve exibir o título correto na página', () => {
    cy.contains('Crie sua Conta').should('be.visible');
  });

  it('Deve mostrar erros de validação ao interagir com campos inválidos', () => {
    // E-mail inválido
    cy.get('input[formControlName="email"]').type('email-invalido').blur();
    cy.contains('Informe um e-mail válido.').should('be.visible');

    // Senha curta
    cy.get('input[formControlName="password"]').type('123').blur();
    cy.contains('A senha deve ter pelo menos 6 caracteres.').should('be.visible');

    // Botão deve estar desabilitado
    cy.get('button[type="submit"]').should('be.disabled');
  });

  it('Deve registrar um novo paciente com sucesso', () => {
    const timestamp = Date.now();
    const email = `test${timestamp}@cypress.com`;

    // Mock da API para evitar criar sujeira no banco real durante desenvolvimento (opcional)
    // Mas aqui vamos testar a integração REAL ou simulada. 
    // Como estamos em ambiente de desenvolvimento local, vamos interceptar se quisermos rapidez.
    
    cy.get('input[formControlName="name"]').type('Cypress Test User');
    cy.get('input[formControlName="email"]').type(email);
    cy.get('input[formControlName="password"]').type('password123');
    cy.get('input[formControlName="cpf"]').type('11122233344');
    cy.get('input[formControlName="whatsapp"]').type('11999998877');
    // O botão deve estar habilitado agora
    cy.get('button[type="submit"]').should('not.be.disabled').click();

    // Verificação de sucesso (mensagem na tela e redirecionamento final)
    cy.get('#success-message', { timeout: 10000 }).should('be.visible')
      .and('contain', 'Paciente registrado com sucesso! Verifique seu e-mail para confirmar a conta.');

    cy.url({ timeout: 10000 }).should('include', '/login');
  });

  it('Deve exibir o banner de verificação pendente após o login', () => {
    // 1. Registrar novo usuário
    const timestamp = Date.now();
    const email = `test_banner_${timestamp}@cypress.com`;
    const password = 'Password123!';

    cy.visit('/register');
    cy.get('input[formControlName="name"]').type('User Banner Test');
    cy.get('input[formControlName="email"]').type(email);
    cy.get('input[formControlName="cpf"]').type('99999999999').blur();
    cy.get('input[formControlName="whatsapp"]').type('11999990000').blur();
    cy.get('input[formControlName="password"]').type(password);
    
    cy.get('button[type="submit"]').click();

    // 2. Login - Ajustado seletores para ID conforme login.html
    cy.visit('/login');
    cy.get('#email').type(email);
    cy.get('#password').type(password);
    cy.get('button[type="submit"]').click();

    // 3. Verificar banner
    cy.url({ timeout: 10000 }).should('include', '/panel');
    cy.get('app-verification-banner', { timeout: 10000 }).should('be.visible')
      .and('contain', 'Seu e-mail ainda não foi verificado');
  });

  it('Deve exibir sucesso na página de verificação de e-mail ao receber token válido', () => {
    cy.intercept('GET', '**/auth/verify?token=token-valido-teste', {
      statusCode: 200,
      body: 'E-mail verificado com sucesso!'
    }).as('verifyRequest');

    cy.visit('/verify-email?token=token-valido-teste');
    
    cy.wait('@verifyRequest');
    cy.contains('Sucesso!').should('be.visible');
    cy.contains('E-mail verificado com sucesso!').should('be.visible');
    cy.get('a[routerLink="/login"]').should('be.visible');
  });

  it('Deve exibir erro ao tentar registrar e-mail já existente', () => {
    // Usamos um e-mail que sabidamente já existe no banco (ex: o admin ou o lucas do seed)
    cy.get('input[formControlName="name"]').type('Usuario Duplicado');
    cy.get('input[formControlName="email"]').type('lucas@email.com');
    cy.get('input[formControlName="password"]').type('senha123');
    cy.get('input[formControlName="cpf"]').type('00000000000');
    cy.get('input[formControlName="whatsapp"]').type('11000000000');

    cy.get('button[type="submit"]').click();

    // Deve aparecer a mensagem de erro retornada pela API
    cy.get('div[class*="bg-red"]', { timeout: 10000 }).should('be.visible');
    cy.contains('Email já cadastrado').should('be.visible');
  });

});
