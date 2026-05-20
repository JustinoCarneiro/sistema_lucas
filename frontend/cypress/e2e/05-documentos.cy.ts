/// <reference types="cypress" />

describe('05 — Documentos (Gestão e Visualização)', () => {

  context('Visão do Profissional', () => {
    const profDocs = [
      {
        id: 1,
        tipo: 'LAUDO_PSICOLOGICO',
        titulo: 'Laudo Psicológico — Lucas Silva',
        nomePaciente: 'Lucas Silva',
        criadoEm: '2026-05-12T10:00:00',
        disponivel: true,
        conteudoTexto: 'Conteúdo do laudo psicológico de exemplo.',
        arquivoBase64: null,
        nomeArquivo: null
      },
      {
        id: 2,
        tipo: 'ATESTADO',
        titulo: 'Atestado de Comparecimento — Maria Oliveira',
        nomePaciente: 'Maria Oliveira',
        criadoEm: '2026-05-09T10:00:00',
        disponivel: true,
        conteudoTexto: 'Atestado de comparecimento.',
        arquivoBase64: null,
        nomeArquivo: null
      },
      {
        id: 3,
        tipo: 'ENCAMINHAMENTO',
        titulo: 'Encaminhamento para Psiquiatria — Lucas Silva',
        nomePaciente: 'Lucas Silva',
        criadoEm: '2026-05-15T10:00:00',
        disponivel: false,
        conteudoTexto: 'Encaminhamento (rascunho).',
        arquivoBase64: null,
        nomeArquivo: null
      }
    ];

    const pacientes = [
      { id: 3, name: 'Lucas Silva' },
      { id: 4, name: 'Maria Oliveira' }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: { consultasHoje: [], proximasConsultas: [], totalPacientes: 0, ultimosProntuarios: [], documentosRecentes: [] }
      }).as('getProfDash');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: profDocs
      }).as('getProfDocs');
      cy.intercept('GET', '**/patients', {
        statusCode: 200,
        body: pacientes
      }).as('getPatients');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/document-management');
      cy.wait('@getProfDocs');
    });

    it('exibe título e botão de novo documento', () => {
      cy.contains('h2', 'Documentos').should('be.visible');
      cy.contains('button', '+ Novo documento').should('be.visible');
    });

    it('lista os documentos do profissional', () => {
      cy.contains('td', 'Laudo Psicológico — Lucas Silva').should('be.visible');
      cy.contains('td', 'Atestado de Comparecimento — Maria Oliveira').should('be.visible');
      cy.contains('td', 'Encaminhamento para Psiquiatria — Lucas Silva').should('be.visible');
    });

    it('exibe rótulo "Sim"/"Não" conforme a disponibilidade', () => {
      cy.contains('td', 'Laudo Psicológico — Lucas Silva').parent('tr')
        .find('button').contains('Sim').should('be.visible');
      cy.contains('td', 'Encaminhamento para Psiquiatria — Lucas Silva').parent('tr')
        .find('button').contains('Não').should('be.visible');
    });

    it('toggle de disponibilidade chama PATCH e recarrega a lista', () => {
      cy.intercept('PATCH', '**/documentos/3/disponibilidade', {
        statusCode: 200,
        body: 'OK'
      }).as('patchDisp');

      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: profDocs.map(d => d.id === 3 ? { ...d, disponivel: true } : d)
      }).as('getProfDocsReload');

      cy.contains('td', 'Encaminhamento para Psiquiatria — Lucas Silva').parent('tr')
        .find('button').contains('Não').click();

      cy.wait('@patchDisp').its('request.body.disponivel').should('eq', true);
      cy.wait('@getProfDocsReload');

      cy.contains('td', 'Encaminhamento para Psiquiatria — Lucas Silva').parent('tr')
        .find('button').contains('Sim').should('be.visible');
    });

    it('abre o modal de detalhes ao clicar em visualizar', () => {
      cy.get('button[title="Visualizar Documento"]').first().click();
      cy.contains('h3', 'Documento').should('be.visible');
      cy.contains('Laudo Psicológico — Lucas Silva').should('be.visible');
      cy.contains('button', 'Fechar').click();
      cy.contains('h3', 'Documento').should('not.exist');
    });

    it('formulário de novo documento abre e exibe selects de paciente/tipo', () => {
      cy.contains('button', '+ Novo documento').click();
      cy.contains('h3', 'Novo documento').should('be.visible');
      cy.contains('label', 'Paciente')
        .siblings('select').find('option').contains('Lucas Silva').should('exist');
      cy.contains('label', 'Tipo de documento')
        .siblings('select').find('option').contains('Laudo Psicológico').should('exist');
    });

    it('submete novo documento via POST /documentos', () => {
      cy.intercept('POST', '**/documentos', {
        statusCode: 201,
        body: { id: 99 }
      }).as('postDoc');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: profDocs
      }).as('reload');

      const alertStub = cy.stub().as('alertStub');
      cy.on('window:alert', alertStub);

      cy.contains('button', '+ Novo documento').click();
      cy.contains('label', 'Paciente').siblings('select').select('Lucas Silva');
      cy.contains('label', 'Tipo de documento').siblings('select').select('Laudo Psicológico');
      cy.contains('label', 'Título').siblings('input').type('Laudo de teste');
      cy.contains('label', /Conteúdo em texto/).siblings('textarea')
        .type('Conteúdo de teste com tamanho suficiente.');
      cy.contains('button', 'Salvar documento').click();

      cy.wait('@postDoc').then(({ request }) => {
        expect(request.body.titulo).to.eq('Laudo de teste');
        expect(request.body.tipo).to.eq('LAUDO_PSICOLOGICO');
      });
      cy.get('@alertStub').should('have.been.calledWithMatch', /Documento criado/);
    });

    it('chama DELETE ao excluir um documento (com confirm aceito)', () => {
      cy.intercept('DELETE', '**/documentos/1', {
        statusCode: 204
      }).as('delDoc');
      cy.intercept('GET', '**/documentos/profissional', {
        statusCode: 200,
        body: profDocs.filter(d => d.id !== 1)
      }).as('reload');

      cy.on('window:confirm', () => true);

      cy.contains('td', 'Laudo Psicológico — Lucas Silva').parent('tr')
        .find('button[title="Excluir Documento"]').click();

      cy.wait('@delDoc');
      cy.wait('@reload');
      cy.contains('td', 'Laudo Psicológico — Lucas Silva').should('not.exist');
    });
  });

  context('Visão do Paciente', () => {
    const meusDocs = [
      {
        id: 1,
        tipo: 'LAUDO_PSICOLOGICO',
        titulo: 'Laudo Psicológico — Lucas Silva',
        nomeProfissional: 'Dra. Ana Souza',
        criadoEm: '2026-05-12T10:00:00',
        conteudoTexto: 'Síntese clínica do laudo psicológico do paciente Lucas Silva.',
        arquivoBase64: null,
        nomeArquivo: null
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/documentos/meus', {
        statusCode: 200,
        body: meusDocs
      }).as('getMeusDocs');
      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-documents');
      cy.wait('@getMeusDocs');
    });

    it('exibe título e botão de portabilidade', () => {
      cy.contains('h2', 'Meus Documentos').should('be.visible');
      cy.contains('button', /Portabilidade/).should('be.visible');
    });

    it('exibe laudo disponibilizado em formato de card', () => {
      cy.contains('h3', 'Laudo Psicológico — Lucas Silva').should('be.visible');
      cy.contains('span', 'Laudo Psicológico').should('be.visible');
      cy.contains('p', 'Dra. Ana Souza').should('be.visible');
    });

    it('botão "Ver completo" expande o conteúdo do documento', () => {
      cy.contains('button', 'Ver completo').click();
      cy.contains('button', 'Recolher').should('be.visible');
    });

    it('documentos não disponíveis (rascunhos) não aparecem na lista', () => {
      cy.contains('Encaminhamento para Psiquiatria').should('not.exist');
    });

    it('estado vazio aparece quando não há documentos', () => {
      cy.intercept('GET', '**/documentos/meus', {
        statusCode: 200,
        body: []
      }).as('getMeusVazio');
      cy.visit('/panel/my-documents');
      cy.wait('@getMeusVazio');
      cy.contains('h3', 'Nenhum documento disponível').should('be.visible');
    });
  });

});
