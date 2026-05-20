/// <reference types="cypress" />

describe('03 — Perfil e Senha', () => {

  context('Visão do Paciente', () => {
    const patientProfile = {
      id: 3,
      name: 'Lucas Silva',
      email: 'lucas@email.com',
      cpf: '111.222.333-44',
      phone: '(11) 88888-9999',
      birthDate: null,
      emergencyContactName: null,
      emergencyContactPhone: null,
      gender: null,
      allergies: null,
      address: null,
      blockedUntil: null,
      infractionCount: 0,
      receivedFirstWarning: false
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/paciente', {
        statusCode: 200,
        body: { totalRealizadas: 0, totalAgendadas: 0, documentosDisponiveis: [], perfil: {} }
      }).as('getPatDash');
      cy.intercept('GET', '**/patients/me', {
        statusCode: 200,
        body: patientProfile
      }).as('getMe');
      cy.login('lucas@email.com', '123456');
      cy.visit('/panel/my-profile');
      cy.wait('@getMe');
    });

    it('carrega dados do paciente nos inputs', () => {
      cy.contains('h2', 'Meu Perfil').should('be.visible');
      cy.get('input[type="text"]').eq(0).should('have.value', 'Lucas Silva');
      cy.get('input[type="email"]').should('have.value', 'lucas@email.com');
      cy.get('input[type="text"]').eq(1).should('have.value', '111.222.333-44');
    });

    it('edita nome e salva (PUT mockado)', () => {
      cy.intercept('PUT', '**/patients/me', {
        statusCode: 200,
        body: 'OK'
      }).as('putMe');

      const alertStub = cy.stub().as('windowAlert');
      cy.on('window:alert', alertStub);

      cy.get('input[type="text"]').eq(0).clear().type('Lucas Silva Editado');
      cy.contains('button', 'Salvar alterações').click();

      cy.wait('@putMe').its('request.body.name').should('eq', 'Lucas Silva Editado');
      cy.get('@windowAlert').should('have.been.calledWithMatch', /Perfil atualizado/);
    });

    it('modal de alteração de senha abre e fecha', () => {
      cy.contains('button', 'Alterar minha senha').click();
      cy.contains('h3', 'Alterar Senha').should('be.visible');
      cy.contains('button', 'Cancelar').click();
      cy.contains('h3', 'Alterar Senha').should('not.exist');
    });

    it('exibe erro quando as senhas não coincidem', () => {
      cy.contains('button', 'Alterar minha senha').click();
      cy.get('input[type="password"]').eq(0).type('novaSenha123');
      cy.get('input[type="password"]').eq(1).type('outraSenha456');
      cy.contains('As senhas não coincidem').should('be.visible');
      cy.contains('button', 'Atualizar').should('be.disabled');
    });

    it('avisa quando a senha é curta', () => {
      cy.contains('button', 'Alterar minha senha').click();
      cy.get('input[type="password"]').eq(0).type('123');
      cy.contains('A senha deve ter pelo menos 6 caracteres').should('be.visible');
    });

    it('habilita Atualizar quando a senha é válida', () => {
      cy.contains('button', 'Alterar minha senha').click();
      cy.get('input[type="password"]').eq(0).type('novaSenha123');
      cy.get('input[type="password"]').eq(1).type('novaSenha123');
      cy.contains('button', 'Atualizar').should('not.be.disabled');
    });

    it('exibe botão de excluir conta', () => {
      cy.contains('button', 'Excluir minha conta').scrollIntoView().should('be.visible');
    });
  });

  context('Visão do Profissional', () => {
    const profProfile = {
      id: 1,
      name: 'Dra. Ana Souza',
      email: 'ana@clinica.com',
      tipoRegistro: 'CRP',
      registroConselho: 'CRP-06 123456',
      specialty: 'Psicologia Clínica',
      cpf: null,
      phone: null,
      birthDate: null,
      gender: null,
      address: null
    };

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/profissional', {
        statusCode: 200,
        body: {
          consultasHoje: [],
          proximasConsultas: [],
          totalPacientes: 0,
          ultimosProntuarios: [],
          documentosRecentes: []
        }
      }).as('getProfDash');
      cy.intercept('GET', '**/professionals/me', {
        statusCode: 200,
        body: profProfile
      }).as('getMe');
      cy.login('ana@clinica.com', '123456');
      cy.visit('/panel/my-profile');
      cy.wait('@getMe');
    });

    it('exibe campos exclusivos do profissional (tipo de registro e número)', () => {
      cy.contains('h2', 'Meu Perfil').should('be.visible');
      cy.contains('label', 'Tipo de registro').should('be.visible');
      cy.get('select').first().should('have.value', 'CRP');
      cy.contains('label', 'Número do registro')
        .siblings('input').should('have.value', 'CRP-06 123456');
      cy.contains('label', 'Especialidade')
        .siblings('input').should('have.value', 'Psicologia Clínica');
    });

    it('exibe campos compartilhados', () => {
      cy.contains('label', 'WhatsApp').should('be.visible');
      cy.contains('label', 'Data de Nascimento').should('be.visible');
      cy.contains('label', 'Gênero').should('be.visible');
      cy.contains('label', 'Endereço completo (com CEP)').should('be.visible');
    });

    it('botão de excluir conta NÃO aparece para profissional', () => {
      cy.contains('button', 'Excluir minha conta').should('not.exist');
    });

    it('salva alterações chamando PUT /professionals/me', () => {
      cy.intercept('PUT', '**/professionals/me', {
        statusCode: 200,
        body: 'OK'
      }).as('putMe');

      const alertStub = cy.stub().as('windowAlert');
      cy.on('window:alert', alertStub);

      cy.contains('label', 'Especialidade').siblings('input')
        .clear().type('Psicologia Clínica e Avaliação');
      cy.contains('button', 'Salvar alterações').click();

      cy.wait('@putMe').its('request.body.specialty')
        .should('eq', 'Psicologia Clínica e Avaliação');
      cy.get('@windowAlert').should('have.been.calledWithMatch', /Perfil atualizado/);
    });
  });

});
