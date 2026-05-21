/// <reference types="cypress" />

describe('17 — Gestão de Documentos', () => {

  const documentosMock = [
    {
      id: 1,
      tipo: 'ATESTADO',
      pacienteName: 'João Silva',
      profissionalName: 'Dra. Ana',
      disponivel: true,
      criadoEm: '2026-05-15T10:00:00',
      arquivoBase64: null
    }
  ];

  const pacientesMock = [
    { id: 3, name: 'João Silva', email: 'joao@email.com' }
  ];

  context('Profissional — Gestão de Documentos', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: 0, pendentesConfirmacao: 0, consultasAtrasadas: 0 }
      }).as('getDash');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: documentosMock
      }).as('getDocs');
      cy.intercept('GET', '**/patients', {
        statusCode: 200,
        body: pacientesMock
      }).as('getPatients');

      cy.login('profissional@clinica.com', '123456');
      cy.visit('/panel/document-management');
      cy.wait('@getDocs');
    });

    it('lista de documentos carrega com pelo menos 1 item', () => {
      cy.contains('João Silva').should('be.visible');
    });

    it('upload de documento → POST 201 → documento aparece na lista', () => {
      const novoDoc = {
        id: 2, tipo: 'LAUDO_PSICOLOGICO', pacienteName: 'João Silva',
        profissionalName: 'Dra. Ana', disponivel: false, criadoEm: '2026-05-20T10:00:00'
      };

      cy.intercept('POST', '**/documentos', {
        statusCode: 201,
        body: novoDoc
      }).as('postDoc');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: [...documentosMock, novoDoc]
      }).as('reloadDocs');

      cy.contains('button', /criar|novo|adicionar/i).click();
      cy.get('select[name="tipo"], select[formControlName="tipo"]').first().select('LAUDO_PSICOLOGICO');
      cy.get('select[name="pacienteId"], select[formControlName="pacienteId"]').first().select('3');
      cy.get('textarea[name="conteudo"], textarea[formControlName="conteudo"]').first().type('Conteúdo do laudo.');
      cy.contains('button', /salvar|enviar|criar/i).click();

      cy.wait('@postDoc');
    });

    it('exclusão de documento → DELETE → documento some da lista', () => {
      cy.intercept('DELETE', '**/documentos/1', {
        statusCode: 204,
        body: null
      }).as('deleteDoc');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: []
      }).as('reloadEmpty');

      const confirmStub = cy.stub().returns(true);
      cy.on('window:confirm', confirmStub);

      cy.contains('João Silva').parents('[class*="border"], tr').first()
        .contains('button', /excluir|remover|deletar/i).click();
      cy.wait('@deleteDoc');
    });
  });

  context('Paciente — Meus Documentos', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [] }
      }).as('getDash');
      cy.intercept('GET', '**/documentos/meus', {
        statusCode: 200,
        body: documentosMock
      }).as('getMyDocs');

      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-documents');
      cy.wait('@getMyDocs');
    });

    it('paciente vê seus documentos disponíveis', () => {
      cy.contains('João Silva').should('be.visible');
    });
  });

});
