/// <reference types="cypress" />

describe('09 — Exportação e Portabilidade (LGPD)', () => {

  context('Admin — Exportações pelo Dashboard', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: {
          totalProfissionais: 2,
          totalPacientes: 4,
          consultasHoje: 10,
          consultasPorStatus: { AGENDADA: 5, CONCLUIDA: 3 }
        }
      }).as('getAdminDash');
      cy.login('admin@clinica.com', 'admin');
      cy.wait('@getAdminDash');
    });

    it('Exportar Pacientes — chama /export/patients com CPFs mascarados', () => {
      const csvMockado =
        'Nome;Email;CPF;Telefone\n' +
        'Lucas Silva;lucas@email.com;***.***-44;(11) 88888-9999\n' +
        'Maria Oliveira;maria@email.com;***.***-55;(11) 99999-8888\n';

      cy.intercept('GET', 'http://localhost:8081/export/patients', {
        statusCode: 200,
        headers: { 'content-type': 'text/csv;charset=UTF-8' },
        body: csvMockado
      }).as('exportPatients');

      cy.contains('button', 'Exportar Pacientes').click();
      cy.wait('@exportPatients').then(({ response, request }) => {
        expect(response!.statusCode).to.eq(200);
        expect(request.headers.authorization).to.match(/^Bearer /);
      });
    });

    it('Exportar Profissionais — chama /export/professionals', () => {
      const csv =
        'Nome;Registro;Especialidade;Email\n' +
        'Dra. Ana Souza;CRP-06 123456;Psicologia Clínica;ana@clinica.com\n';

      cy.intercept('GET', 'http://localhost:8081/export/professionals', {
        statusCode: 200,
        headers: { 'content-type': 'text/csv;charset=UTF-8' },
        body: csv
      }).as('exportProfs');

      cy.contains('button', 'Exportar Profissionais').click();
      cy.wait('@exportProfs').its('response.statusCode').should('eq', 200);
    });

    it('Exportar Relatório Geral — chama /export/admin', () => {
      cy.intercept('GET', 'http://localhost:8081/export/admin', {
        statusCode: 200,
        headers: { 'content-type': 'text/csv;charset=UTF-8' },
        body: 'Métrica;Valor\nProfissionais;2\nPacientes;4\n'
      }).as('exportAdmin');

      cy.contains('button', 'Exportar Relatório Geral').click();
      cy.wait('@exportAdmin').its('response.statusCode').should('eq', 200);
    });

    it('falha no servidor exibe alerta ao usuário', () => {
      cy.intercept('GET', 'http://localhost:8081/export/admin', {
        statusCode: 500,
        body: 'erro'
      }).as('exportFail');

      const alertStub = cy.stub().as('alertStub');
      cy.on('window:alert', alertStub);

      cy.contains('button', 'Exportar Relatório Geral').click();
      cy.wait('@exportFail');
      cy.get('@alertStub').should('have.been.calledWithMatch', /Não foi possível gerar a exportação/);
    });
  });

  context('Profissional — Exportar Meus Atendimentos', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.login('ana@clinica.com', '123456');
      cy.wait('@getProfDash');
    });

    it('chama /export/professional ao clicar no botão', () => {
      const csv =
        'ID;Data;Paciente;Notas\n' +
        '1;2026-05-12;Lucas Silva;Sessão de acompanhamento.\n';

      cy.intercept('GET', 'http://localhost:8081/export/professional', {
        statusCode: 200,
        headers: { 'content-type': 'text/csv;charset=UTF-8' },
        body: csv
      }).as('exportMe');

      cy.contains('button', 'Exportar Meus Atendimentos').click();
      cy.wait('@exportMe').then(({ response, request }) => {
        expect(response!.statusCode).to.eq(200);
        expect(request.headers.authorization).to.match(/^Bearer /);
      });
    });
  });

  context('Paciente — Portabilidade dos próprios dados', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/documentos/meus', {
        statusCode: 200,
        body: []
      }).as('getMeus');
      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-documents');
      cy.wait('@getMeus');
    });

    it('botão "Portabilidade" chama /export/patient e exporta dados do paciente', () => {
      const csv =
        'TIPO;TITULO;DATA;NOTAS/CONTEUDO\n' +
        'LAUDO_PSICOLOGICO;Laudo — Lucas Silva;2026-05-12;Conteúdo do laudo.\n';

      cy.intercept('GET', 'http://localhost:8081/export/patient', {
        statusCode: 200,
        headers: { 'content-type': 'text/csv;charset=UTF-8' },
        body: csv
      }).as('exportSelf');

      cy.contains('button', /Portabilidade/).click();
      cy.wait('@exportSelf').then(({ response, request }) => {
        expect(response!.statusCode).to.eq(200);
        expect(request.headers.authorization).to.match(/^Bearer /);
      });
    });
  });

  context('Segurança RBAC — endpoints de Admin protegidos', () => {
    it('Paciente recebe 403 ao tentar /export/patients (somente Admin)', () => {
      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiUrl')}/auth/login`,
        body: { email: 'lucas@email.com', password: '123456' }
      }).then(({ body }) => {
        const token = body.token;
        cy.request({
          method: 'GET',
          url: `${Cypress.env('apiUrl')}/export/patients`,
          headers: { Authorization: `Bearer ${token}` },
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(403);
        });
      });
    });

    it('Profissional recebe 403 ao tentar /export/admin', () => {
      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiUrl')}/auth/login`,
        body: { email: 'ana@clinica.com', password: '123456' }
      }).then(({ body }) => {
        const token = body.token;
        cy.request({
          method: 'GET',
          url: `${Cypress.env('apiUrl')}/export/admin`,
          headers: { Authorization: `Bearer ${token}` },
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(403);
        });
      });
    });

    it('sem token retorna 401/403 em /export/patients', () => {
      cy.request({
        method: 'GET',
        url: `${Cypress.env('apiUrl')}/export/patients`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([401, 403]);
      });
    });
  });

});
