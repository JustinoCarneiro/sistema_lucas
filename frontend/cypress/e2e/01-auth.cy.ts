/// <reference types="cypress" />

describe('01 — Autenticação', () => {

  // JWT sintético — o auth.guard apenas decodifica payload e cheka exp.
  // Não precisa ser assinado pelo backend.
  function makeJwt(payload: Record<string, any>): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body   = btoa(JSON.stringify(payload));
    return `${header}.${body}.assinatura-falsa`;
  }

  function tokenPara(role: 'ADMIN' | 'PROFESSIONAL' | 'PATIENT', email: string): string {
    return makeJwt({
      sub: email,
      role,
      verified: true,
      exp: Math.floor(Date.now() / 1000) + 3600
    });
  }

  // ────────────────────────────────────────────────────────────────────────
  // CENÁRIOS NEGATIVOS — executam primeiro para evitar consumo desnecessário
  // do rate-limit ou de fluxos felizes acidentalmente disparados.
  // ────────────────────────────────────────────────────────────────────────
  context('Cenários negativos e validação client-side', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
      cy.visit('/login');
    });

    it('botão "Entrar no sistema" começa desabilitado com formulário vazio', () => {
      cy.contains('button', /Entrar no sistema/).should('be.disabled');
    });

    it('exibe mensagem "E-mail inválido." quando formato do e-mail é inválido', () => {
      cy.get('input#email').type('email-quebrado').blur();
      cy.contains('E-mail inválido.').should('be.visible');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('rejeita senha errada (401 com mensagem) e exibe alerta vermelho', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 401,
        body: { message: 'E-mail ou senha inválidos.' }
      }).as('loginErrado');

      cy.get('input#email').type('lucas@email.com');
      cy.get('input#password').type('senhaErrada');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginErrado').its('response.statusCode').should('eq', 401);
      cy.get('div[class*="bg-red"]').should('be.visible')
        .and('contain', 'E-mail ou senha inválidos.');
      cy.url().should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'token').should('be.null');
    });

    it('rejeita e-mail inexistente (400) com mensagem do backend', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 400,
        body: { message: 'Usuário não encontrado.' }
      }).as('loginInexistente');

      cy.get('input#email').type('naoexiste@fake.com');
      cy.get('input#password').type('qualquerCoisa');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginInexistente');
      cy.get('div[class*="bg-red"]').should('be.visible')
        .and('contain', 'Usuário não encontrado.');
      cy.url().should('include', '/login');
    });

    it('fallback genérico quando o backend não devolve mensagem (500)', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 500,
        body: {}
      }).as('login500');

      cy.get('input#email').type('lucas@email.com');
      cy.get('input#password').type('123456');
      cy.get('button[type="submit"]').click();

      cy.wait('@login500');
      cy.get('div[class*="bg-red"]').should('be.visible')
        .and('contain', 'E-mail ou senha inválidos.');
    });

    it('estado de loading exibe "Entrando..." durante a requisição', () => {
      cy.intercept('POST', '**/auth/login', (req) => {
        req.reply({ delay: 600, statusCode: 401, body: { message: 'Bloqueado' } });
      }).as('loginLento');

      cy.get('input#email').type('lucas@email.com');
      cy.get('input#password').type('123456');
      cy.get('button[type="submit"]').click();
      cy.contains('button', 'Entrando...').should('be.visible').and('be.disabled');
      cy.wait('@loginLento');
    });
  });

  // ────────────────────────────────────────────────────────────────────────
  // NAVEGAÇÃO — links secundários da página de login
  // ────────────────────────────────────────────────────────────────────────
  context('Navegação a partir da tela de login', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
      cy.visit('/login');
    });

    it('link "Crie a sua conta aqui" navega para /register', () => {
      cy.contains('a', 'Crie a sua conta aqui').click();
      cy.url().should('include', '/register');
      cy.contains('h2', 'Crie sua Conta').should('be.visible');
    });

    it('link "Esqueceu a senha?" navega para /forgot-password', () => {
      cy.contains('a', 'Esqueceu a senha?').click();
      cy.url().should('include', '/forgot-password');
      cy.contains('h2', 'Esqueceu a senha?').should('be.visible');
    });
  });

  // ────────────────────────────────────────────────────────────────────────
  // LOGINS POSITIVOS POR ROLE — mock de POST /auth/login + JWT sintético
  // que satisfaz o authGuard e o AuthService.getUserRole()
  // ────────────────────────────────────────────────────────────────────────
  context('Login bem-sucedido por role', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
    });

    it('PACIENTE (Lucas) é redirecionado para /panel/dashboard', () => {
      const token = tokenPara('PATIENT', 'lucas@email.com');

      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: { token }
      }).as('loginPaciente');
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getDash');
      cy.intercept('GET', 'http://localhost:8081/patients/me', {
        statusCode: 200,
        body: { id: 3, name: 'Lucas Silva', email: 'lucas@email.com' }
      }).as('getMe');

      cy.visit('/login');
      cy.get('input#email').type('lucas@email.com');
      cy.get('input#password').type('123456');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginPaciente').its('request.body').should('deep.include', {
        email: 'lucas@email.com',
        password: '123456'
      });
      cy.url({ timeout: 10000 }).should('include', '/panel/dashboard');
      cy.wait('@getDash');
      cy.window().its('localStorage').invoke('getItem', 'token').should('eq', token);
    });

    it('PROFISSIONAL (Dra. Ana) é redirecionada para /panel/dashboard', () => {
      const token = tokenPara('PROFESSIONAL', 'ana@clinica.com');

      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: { token }
      }).as('loginProf');
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getDash');
      cy.intercept('GET', 'http://localhost:8081/professionals/me', {
        statusCode: 200,
        body: { id: 1, name: 'Dra. Ana Souza', email: 'ana@clinica.com' }
      }).as('getMe');

      cy.visit('/login');
      cy.get('input#email').type('ana@clinica.com');
      cy.get('input#password').type('123456');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginProf');
      cy.url({ timeout: 10000 }).should('include', '/panel/dashboard');
      cy.wait('@getDash');
      cy.window().its('localStorage').invoke('getItem', 'token').should('eq', token);
    });

    it('ADMIN é redirecionado para /panel/dashboard', () => {
      const token = tokenPara('ADMIN', 'admin@clinica.com');

      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: { token }
      }).as('loginAdmin');
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 0, totalPacientes: 0, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getDash');

      cy.visit('/login');
      cy.get('input#email').type('admin@clinica.com');
      cy.get('input#password').type('admin');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginAdmin');
      cy.url({ timeout: 10000 }).should('include', '/panel/dashboard');
      cy.wait('@getDash');
      cy.window().its('localStorage').invoke('getItem', 'token').should('eq', token);
    });
  });

  // ────────────────────────────────────────────────────────────────────────
  // RATE LIMITING E SESSÃO
  // ────────────────────────────────────────────────────────────────────────
  context('Rate limiting e sessão expirada', () => {
    beforeEach(() => {
      cy.clearLocalStorage();
      cy.visit('/login');
    });

    it('backend retorna 429 (Too Many Requests) → exibe mensagem de bloqueio temporário', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 429,
        body: { message: 'Muitas tentativas. Tente novamente em alguns minutos.' }
      }).as('loginRateLimit');

      cy.get('input#email').type('lucas@email.com');
      cy.get('input#password').type('senhaErrada');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginRateLimit');
      cy.get('div[class*="bg-red"]').should('be.visible');
      cy.url().should('include', '/login');
    });

    it('token expirado no localStorage → authGuard redireciona para /login', () => {
      const expiredToken = (() => {
        const h = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
        const p = btoa(JSON.stringify({ sub: 'lucas@email.com', role: 'PATIENT', verified: true, exp: 1 }));
        return `${h}.${p}.fake`;
      })();

      cy.window().then(win => win.localStorage.setItem('token', expiredToken));
      cy.visit('/panel/dashboard');
      cy.url({ timeout: 8000 }).should('include', '/login');
    });

    it('login com conta não verificada → backend retorna 403 → exibe mensagem de erro', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 403,
        body: { message: 'E-mail não verificado. Verifique sua caixa de entrada.' }
      }).as('loginUnverified');

      cy.get('input#email').type('novo@email.com');
      cy.get('input#password').type('senha123');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginUnverified');
      cy.get('div[class*="bg-red"]').should('be.visible');
      cy.url().should('include', '/login');
    });
  });

  // ────────────────────────────────────────────────────────────────────────
  // LOGOUT — limpa token e redireciona para /login
  // ────────────────────────────────────────────────────────────────────────
  context('Logout', () => {
    it('botão "Sair do sistema" limpa o token e redireciona para /login', () => {
      const token = tokenPara('PATIENT', 'lucas@email.com');

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
          win.localStorage.setItem('token', token);
        }
      });
      cy.visit('/panel/dashboard');
      cy.wait('@getDash');

      cy.contains('button', 'Sair do sistema').scrollIntoView().click();

      cy.url({ timeout: 10000 }).should('include', '/login');
      cy.window().its('localStorage').invoke('getItem', 'token').should('be.null');
    });
  });

});
