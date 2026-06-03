/// <reference types="cypress" />

describe('10 — Registro, Verificação e Recuperação de Senha', () => {

  // ─── Helper para gerar JWT sintético ────────────────────────
  function makeFakeJwt(payload: Record<string, any>): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body   = btoa(JSON.stringify(payload));
    return `${header}.${body}.assinatura-falsa`;
  }

  context('Página de Registro', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
      cy.visit('/register');
    });

    it('exibe o título da página', () => {
      cy.contains('h2', 'Crie sua Conta').should('be.visible');
    });

    it('mostra erro quando e-mail é inválido', () => {
      cy.get('input[formControlName="email"]').type('email-invalido').blur();
      cy.contains('Informe um e-mail válido.').should('be.visible');
    });

    it('mostra erro quando senha tem menos de 6 caracteres', () => {
      cy.get('input[formControlName="password"]').type('123').blur();
      cy.contains('A senha deve ter pelo menos 6 caracteres.').should('be.visible');
    });

    it('botão Cadastrar fica desabilitado quando o formulário é inválido', () => {
      cy.get('button[type="submit"]').should('be.disabled');
      cy.get('input[formControlName="name"]').type('Teste');
      cy.get('input[formControlName="email"]').type('valido@email.com');
      cy.get('input[formControlName="password"]').type('senha123');
      // ainda falta CPF e WhatsApp
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('aplica máscara automática no campo CPF', () => {
      cy.get('input[formControlName="cpf"]').type('12345678901');
      cy.get('input[formControlName="cpf"]').should('have.value', '123.456.789-01');
    });

    it('aplica máscara automática no campo WhatsApp', () => {
      cy.get('input[formControlName="whatsapp"]').type('11999998877');
      cy.get('input[formControlName="whatsapp"]').should('have.value', '(11) 99999-8877');
    });

    it('registra paciente com sucesso e redireciona para /login', () => {
      cy.intercept('POST', '**/auth/register', {
        statusCode: 200,
        body: 'Paciente registrado com sucesso! Verifique seu e-mail para confirmar a conta.'
      }).as('postRegister');

      cy.get('input[formControlName="name"]').type('Cypress Test User');
      cy.get('input[formControlName="email"]').type('cypress@test.com');
      cy.get('input[formControlName="password"]').type('senha123');
      cy.get('input[formControlName="cpf"]').type('11122233344');
      cy.get('input[formControlName="whatsapp"]').type('11999998877');
      cy.get('input[type="checkbox"]#termsAccepted').check();

      cy.get('button[type="submit"]').should('not.be.disabled').click();

      cy.wait('@postRegister').then(({ request }) => {
        expect(request.body.email).to.eq('cypress@test.com');
        expect(request.body.role).to.eq('PATIENT');
        expect(request.body.phone).to.match(/^\(11\)/);
      });

      cy.get('#success-message').should('be.visible')
        .and('contain', 'Paciente registrado com sucesso');

      cy.url({ timeout: 6000 }).should('include', '/login');
    });

    it('checkbox LGPD obrigatório — formulário inválido sem aceite', () => {
      cy.get('input[formControlName="name"]').type('Teste LGPD');
      cy.get('input[formControlName="email"]').type('lgpd@test.com');
      cy.get('input[formControlName="password"]').type('senha123');
      cy.get('input[formControlName="cpf"]').type('11122233344');
      cy.get('input[formControlName="whatsapp"]').type('11999998877');
      // checkbox termsAccepted ainda não foi marcado
      cy.get('button[type="submit"]').should('be.disabled');

      cy.get('input[type="checkbox"]#termsAccepted').check();
      cy.get('button[type="submit"]').should('not.be.disabled');
    });

    it('CPF duplicado → 409 com mensagem de CPF já cadastrado', () => {
      cy.intercept('POST', '**/auth/register', {
        statusCode: 409,
        body: { message: 'CPF já cadastrado.' }
      }).as('postRegisterCpfDup');

      cy.get('input[formControlName="name"]').type('Outro Nome');
      cy.get('input[formControlName="email"]').type('outro@email.com');
      cy.get('input[formControlName="password"]').type('senha123');
      cy.get('input[formControlName="cpf"]').type('11122233344');
      cy.get('input[formControlName="whatsapp"]').type('11999998877');
      cy.get('input[type="checkbox"]#termsAccepted').check();

      cy.get('button[type="submit"]').click();
      cy.wait('@postRegisterCpfDup');

      cy.contains('CPF já cadastrado').should('be.visible');
    });

    it('mostra mensagem de erro quando e-mail já existe', () => {
      cy.intercept('POST', '**/auth/register', {
        statusCode: 409,
        body: { message: 'Email já cadastrado.' }
      }).as('postRegister');

      cy.get('input[formControlName="name"]').type('Duplicado');
      cy.get('input[formControlName="email"]').type('lucas@email.com');
      cy.get('input[formControlName="password"]').type('senha123');
      cy.get('input[formControlName="cpf"]').type('11122233344');
      cy.get('input[formControlName="whatsapp"]').type('11999998877');
      cy.get('input[type="checkbox"]#termsAccepted').check();

      cy.get('button[type="submit"]').click();
      cy.wait('@postRegister');

      cy.get('div[class*="bg-red-50"]').should('be.visible');
      cy.contains('Email já cadastrado').should('be.visible');
    });
  });

  context('Verificação de E-mail', () => {
    it('exibe sucesso ao receber token válido', () => {
      cy.intercept('GET', '**/auth/verify*', {
        statusCode: 200,
        body: 'E-mail verificado com sucesso!'
      }).as('verifyOk');

      cy.visit('/verify-email?token=token-valido');
      cy.wait('@verifyOk');
      cy.contains('h2', 'Sucesso!').should('be.visible');
      cy.contains('E-mail verificado com sucesso!').should('be.visible');
      cy.get('a[routerLink="/login"]').should('be.visible');
    });

    it('exibe erro quando token é inválido/expirado', () => {
      cy.intercept('GET', '**/auth/verify*', {
        statusCode: 400,
        body: 'Token inválido ou expirado.'
      }).as('verifyFail');

      cy.visit('/verify-email?token=token-quebrado');
      cy.wait('@verifyFail');
      cy.contains('h2', 'Ops!').should('be.visible');
    });

    it('mostra mensagem quando não há token na URL', () => {
      cy.visit('/verify-email');
      cy.contains('Token de verificação não encontrado.').should('be.visible');
      cy.contains('h2', 'Ops!').should('be.visible');
    });
  });

  context('Banner de Verificação Pendente', () => {
    it('exibe banner quando paciente está logado com verified=false', () => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getDash');
      cy.intercept('GET', 'http://localhost:8081/patients/me', {
        statusCode: 200,
        body: { id: 99, name: 'Novo Paciente', email: 'novo@email.com' }
      }).as('getMe');

      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('role', 'PATIENT');
          win.localStorage.setItem('verified', 'false');
        }
      });
      cy.visit('/panel/dashboard');
      cy.wait('@getDash');

      cy.get('app-verification-banner').should('be.visible')
        .and('contain', 'Seu e-mail ainda não foi verificado');
    });

    it('NÃO exibe banner quando verified=true', () => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getDash');
      cy.intercept('GET', 'http://localhost:8081/patients/me', {
        statusCode: 200,
        body: { id: 3, name: 'Lucas Silva', email: 'lucas@email.com' }
      }).as('getMe');

      cy.visit('/login', {
        onBeforeLoad(win) {
          win.localStorage.setItem('role', 'PATIENT');
          win.localStorage.setItem('verified', 'true');
        }
      });
      cy.visit('/panel/dashboard');
      cy.wait('@getDash');

      cy.get('app-verification-banner').children().should('not.exist');
    });
  });

  context('Esqueci minha senha', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
      cy.visit('/forgot-password');
    });

    it('exibe formulário com campo de e-mail', () => {
      cy.contains('h2', 'Esqueceu a senha?').should('be.visible');
      cy.get('input[formControlName="email"]').should('be.visible');
      cy.contains('button', /Enviar link/).should('be.visible');
    });

    it('exibe erro quando e-mail é inválido', () => {
      cy.get('input[formControlName="email"]').type('email-invalido').blur();
      cy.contains('Informe um e-mail válido.').should('be.visible');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('mostra confirmação genérica após envio (não revela existência do e-mail)', () => {
      cy.intercept('POST', '**/auth/esqueci-senha', {
        statusCode: 200,
        body: 'OK'
      }).as('postForgot');

      cy.get('input[formControlName="email"]').type('qualquer@email.com');
      cy.contains('button', /Enviar link/).click();

      cy.wait('@postForgot');
      cy.contains('Verifique seu e-mail').should('be.visible');
      cy.contains('Se este e-mail estiver cadastrado').should('be.visible');
    });
  });

  context('Redefinir senha (reset-password)', () => {
    it('mostra mensagem "Link inválido" sem token', () => {
      cy.visit('/reset-password');
      cy.contains('h2', 'Link inválido').should('be.visible');
    });

    it('exibe formulário quando token está presente', () => {
      cy.visit('/reset-password?token=token-de-teste');
      cy.contains('h2', 'Nova senha').should('be.visible');
      cy.get('input[formControlName="novaSenha"]').should('be.visible');
      cy.get('input[formControlName="confirmarSenha"]').should('be.visible');
    });

    it('exibe erro quando senhas não coincidem', () => {
      cy.visit('/reset-password?token=token-de-teste');
      cy.get('input[formControlName="novaSenha"]').type('senha123');
      cy.get('input[formControlName="confirmarSenha"]').type('outraSenha456').blur();
      cy.contains('As senhas não coincidem').should('be.visible');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('redefine senha com sucesso e exibe confirmação', () => {
      cy.intercept('POST', '**/auth/redefinir-senha', {
        statusCode: 200,
        body: 'OK'
      }).as('postReset');

      cy.visit('/reset-password?token=token-valido');
      cy.get('input[formControlName="novaSenha"]').type('novaSenha123');
      cy.get('input[formControlName="confirmarSenha"]').type('novaSenha123');
      cy.contains('button', 'Redefinir senha').click();

      cy.wait('@postReset').then(({ request }) => {
        expect(request.body.token).to.eq('token-valido');
        expect(request.body.novaSenha).to.eq('novaSenha123');
      });

      cy.contains('Senha redefinida!').should('be.visible');
    });

    it('exibe erro quando o token expirou no backend', () => {
      cy.intercept('POST', '**/auth/redefinir-senha', {
        statusCode: 400,
        body: 'Link inválido ou expirado.'
      }).as('postResetFail');

      cy.visit('/reset-password?token=token-expirado');
      cy.get('input[formControlName="novaSenha"]').type('novaSenha123');
      cy.get('input[formControlName="confirmarSenha"]').type('novaSenha123');
      cy.contains('button', 'Redefinir senha').click();
      cy.wait('@postResetFail');

      cy.contains('Link inválido ou expirado').should('be.visible');
    });
  });

});
