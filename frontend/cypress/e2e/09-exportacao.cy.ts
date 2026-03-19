/// <reference types="cypress" />

describe('09 — Exportação e Portabilidade (LGPD)', () => {

  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('Admin — Exportar Pacientes (Mascaramento CPF)', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/dashboard');

    // Intercepta a chamada de exportação
    cy.intercept('GET', '**/export/patients').as('exportPatients');

    // Clica no botão de exportar pacientes
    cy.contains('button', 'Exportar Pacientes').click();

    // Valida que a requisição foi feita e a resposta contém CPFs mascarados
    cy.wait('@exportPatients').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.contain('***.***-'); // Garante mascaramento
      expect(interception.response.headers['content-type']).to.contain('text/csv');
    });
  });

  it('Admin — Exportar Profissionais', () => {
    cy.login('admin@clinica.com', 'admin123');
    cy.visit('/panel/dashboard');

    cy.intercept('GET', '**/export/professionals').as('exportProfs');
    cy.contains('button', 'Exportar Profissionais').click();

    cy.wait('@exportProfs').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.contain('Registro;Especialidade;Email');
      expect(interception.response.body).to.contain('Dra. Ana Souza');
    });
  });

  it('Profissional — Exportar Meus Atendimentos', () => {
    cy.login('ana@clinica.com', '123456');
    cy.visit('/panel/dashboard');

    cy.intercept('GET', '**/export/professional').as('exportMe');
    cy.contains('button', 'Exportar Meus Atendimentos').click();

    cy.wait('@exportMe').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.contain('ID;Data;Paciente;Notas');
    });
  });

  it('Paciente — Portabilidade (Exportar Meus Dados)', () => {
    cy.login('lucas@email.com', '123456');
    cy.visit('/panel/my-documents');

    cy.intercept('GET', '**/export/patient').as('exportSelf');
    cy.contains('button', 'Portabilidade').click();

    cy.wait('@exportSelf').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.contain('TIPO;TITULO;DATA;NOTAS/CONTEUDO');
      expect(interception.response.body).to.contain('Lucas Silva');
    });
  });

  it('Segurança — Paciente não deve acessar exportação Admin', () => {
    cy.login('lucas@email.com', '123456');
    
    // Tenta acessar diretamente o endpoint de Admin
    cy.request({
      url: 'http://localhost:8081/export/patients',
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.eq(403); // Forbidden
    });
  });

});
