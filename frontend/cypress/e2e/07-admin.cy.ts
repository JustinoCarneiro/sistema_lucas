/// <reference types="cypress" />

describe('07 — Painel Administrativo', () => {

  context('Profissionais (Corpo Clínico)', () => {
    const professionals = [
      {
        id: 1,
        name: 'Dra. Ana Souza',
        email: 'ana@clinica.com',
        specialty: 'Psicologia Clínica',
        tipoRegistro: 'CRP',
        registroConselho: 'CRP-06 123456'
      },
      {
        id: 2,
        name: 'Dr. Carlos Menezes',
        email: 'carlos@clinica.com',
        specialty: 'Psiquiatria',
        tipoRegistro: 'CRM',
        registroConselho: 'CRM-SP 654321'
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 2, totalPacientes: 0, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getAdminDash');
      cy.intercept('GET', 'http://localhost:8081/professionals', {
        statusCode: 200,
        body: professionals
      }).as('getProfs');
      cy.login('admin@clinica.com', 'admin');
      cy.visit('/panel/professionals');
      cy.wait('@getProfs');
    });

    it('exibe título e tabela com profissionais', () => {
      cy.contains('h2', 'Corpo Clínico').should('be.visible');
      cy.contains('td', 'Dra. Ana Souza').should('be.visible');
      cy.contains('td', 'Dr. Carlos Menezes').should('be.visible');
      cy.contains('td', 'Psicologia Clínica').should('be.visible');
      cy.contains('td', 'Psiquiatria').should('be.visible');
    });

    it('exibe registros (CRP/CRM) na tabela', () => {
      cy.contains('td', /CRP\s*CRP-06 123456/).should('be.visible');
      cy.contains('td', /CRM\s*CRM-SP 654321/).should('be.visible');
    });

    it('abre formulário de novo profissional ao clicar no botão', () => {
      cy.contains('button', '+ Novo profissional').click();
      cy.contains('h3', 'Novo Profissional').should('be.visible');
      cy.get('input[formControlName="name"]').should('be.visible');
      cy.get('input[formControlName="email"]').should('be.visible');
      cy.get('select[formControlName="tipoRegistro"]').should('be.visible');
      cy.get('input[formControlName="registroConselho"]').should('be.visible');
      cy.get('input[formControlName="specialty"]').should('be.visible');
      cy.get('input[formControlName="password"]').should('be.visible');
    });

    it('submete POST /professionals com payload correto', () => {
      cy.intercept('POST', '**/professionals', {
        statusCode: 201,
        body: 'OK'
      }).as('postProf');
      cy.contains('button', '+ Novo profissional').click();
      cy.get('input[formControlName="name"]').type('Dra. Beatriz Lima');
      cy.get('input[formControlName="email"]').type('beatriz@clinica.com');
      cy.get('select[formControlName="tipoRegistro"]').select('CRP');
      cy.get('input[formControlName="registroConselho"]').type('CRP-06 999999');
      cy.get('input[formControlName="specialty"]').type('Neuropsicologia');
      cy.get('input[formControlName="password"]').type('senhaForte123');

      cy.contains('button', 'Cadastrar').click();

      cy.wait('@postProf').then(({ request }) => {
        expect(request.body.name).to.eq('Dra. Beatriz Lima');
        expect(request.body.email).to.eq('beatriz@clinica.com');
        expect(request.body.tipoRegistro).to.eq('CRP');
        expect(request.body.registroConselho).to.eq('CRP-06 999999');
      });
      cy.contains('[role="alert"]', /cadastrado com sucesso/).should('be.visible');
    });

    it('clique em "Editar" pré-preenche o formulário com dados do profissional', () => {
      cy.contains('tr', 'Dra. Ana Souza').contains('button', 'Editar').click();
      cy.contains('h3', 'Editar Profissional').should('be.visible');
      cy.get('input[formControlName="name"]').should('have.value', 'Dra. Ana Souza');
      cy.get('input[formControlName="email"]').should('have.value', 'ana@clinica.com');
      cy.get('input[formControlName="specialty"]').should('have.value', 'Psicologia Clínica');
    });

    it('atualiza profissional via PUT /professionals/{id}', () => {
      cy.intercept('PUT', '**/professionals/1', {
        statusCode: 200,
        body: 'OK'
      }).as('putProf');
      cy.contains('tr', 'Dra. Ana Souza').contains('button', 'Editar').click();
      cy.get('input[formControlName="specialty"]').clear().type('Psicologia Clínica e Avaliação');
      cy.contains('button', 'Salvar alterações').click();

      cy.wait('@putProf').its('request.body.specialty')
        .should('eq', 'Psicologia Clínica e Avaliação');
      cy.contains('[role="alert"]', /atualizado com sucesso/).should('be.visible');
    });

    it('tenta excluir profissional com consultas ativas → DELETE force retorna 409 → exibe erro', () => {
      cy.intercept('DELETE', '**/professionals/force/1', {
        statusCode: 409,
        body: { message: 'Não é possível excluir: profissional possui consultas ativas.' }
      }).as('delConflict');
      cy.on('window:confirm', () => true);

      cy.contains('tr', 'Dra. Ana Souza').contains('button', 'Excluir').click();

      cy.wait('@delConflict');
      cy.contains('[role="alert"]', /Não é possível excluir|consultas ativas/).should('be.visible');
    });

    it('exclui profissional via DELETE /professionals/force/{id} (com confirm)', () => {
      cy.intercept('DELETE', '**/professionals/force/2', {
        statusCode: 204
      }).as('delProf');
      cy.intercept('GET', 'http://localhost:8081/professionals', {
        statusCode: 200,
        body: professionals.filter(p => p.id !== 2)
      }).as('reload');
      cy.on('window:confirm', () => true);

      cy.contains('tr', 'Dr. Carlos Menezes').contains('button', 'Excluir').click();

      cy.wait('@delProf');
      cy.wait('@reload');
      cy.contains('[role="alert"]', /removido com sucesso/).should('be.visible');
      cy.contains('td', 'Dr. Carlos Menezes').should('not.exist');
    });
  });

  context('Pacientes (Gestão Admin)', () => {
    const blockedFuture = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const patients = [
      {
        id: 3,
        name: 'Lucas Silva',
        email: 'lucas@email.com',
        cpf: '111.222.333-44',
        phone: '(11) 88888-9999',
        blockedUntil: null,
        infractionCount: 1,
        receivedFirstWarning: true
      },
      {
        id: 4,
        name: 'Maria Oliveira',
        email: 'maria@email.com',
        cpf: '222.333.444-55',
        phone: '(11) 99999-8888',
        blockedUntil: blockedFuture,
        infractionCount: 3,
        receivedFirstWarning: true
      },
      {
        id: 5,
        name: 'João Pereira',
        email: 'joao@email.com',
        cpf: '333.444.555-66',
        phone: '(11) 99999-0003',
        blockedUntil: null,
        infractionCount: 0,
        receivedFirstWarning: false
      }
    ];

    beforeEach(() => {
      cy.intercept('GET', '**/dashboard/admin', {
        statusCode: 200,
        body: { totalProfissionais: 0, totalPacientes: 3, consultasHoje: 0, consultasPorStatus: {} }
      }).as('getAdminDash');
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: patients
      }).as('getPatients');
      cy.login('admin@clinica.com', 'admin');
      cy.visit('/panel/patients');
      cy.wait('@getPatients');
    });

    it('exibe lista completa por padrão (filtro TODOS)', () => {
      cy.contains('h2', 'Pacientes').should('be.visible');
      cy.contains('td', 'Lucas Silva').should('be.visible');
      cy.contains('td', 'Maria Oliveira').should('be.visible');
      cy.contains('td', 'João Pereira').should('be.visible');
    });

    it('filtro "Bloqueados" mostra apenas pacientes bloqueados', () => {
      cy.contains('button', 'Bloqueados Temporariamente').click();
      cy.contains('td', 'Maria Oliveira').should('be.visible');
      cy.contains('td', 'Lucas Silva').should('not.exist');
      cy.contains('td', 'João Pereira').should('not.exist');
    });

    it('filtro "Com Histórico de Ausências" mostra apenas com infrações', () => {
      cy.contains('button', 'Com Histórico de Ausências').click();
      cy.contains('td', 'Lucas Silva').should('be.visible');
      cy.contains('td', 'Maria Oliveira').should('be.visible');
      cy.contains('td', 'João Pereira').should('not.exist');
    });

    it('desbloqueia paciente via PATCH /patients/{id}/desbloquear', () => {
      cy.intercept('PATCH', '**/patients/4/desbloquear', {
        statusCode: 200,
        body: 'OK'
      }).as('desbloq');
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: patients.map(p => p.id === 4 ? { ...p, blockedUntil: null } : p)
      }).as('reload');
      cy.on('window:confirm', () => true);

      cy.contains('tr', 'Maria Oliveira').contains('button', 'Desbloquear').click();

      cy.wait('@desbloq');
      cy.wait('@reload');
      cy.contains('[role="alert"]', /desbloqueado com sucesso/).should('be.visible');
    });

    it('remove paciente via DELETE /patients/{id} (com confirm)', () => {
      cy.intercept('DELETE', '**/patients/5', {
        statusCode: 204
      }).as('delPat');
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: patients.filter(p => p.id !== 5)
      }).as('reload');
      cy.on('window:confirm', () => true);

      cy.contains('tr', 'João Pereira').contains('button', 'Remover').click();

      cy.wait('@delPat');
      cy.wait('@reload');
      cy.contains('[role="alert"]', /removido com sucesso/).should('be.visible');
      cy.contains('td', 'João Pereira').should('not.exist');
    });

    it('exibe estado vazio quando nenhum paciente passa no filtro', () => {
      cy.intercept('GET', 'http://localhost:8081/patients', {
        statusCode: 200,
        body: []
      }).as('getEmpty');
      cy.visit('/panel/patients');
      cy.wait('@getEmpty');
      cy.contains('Nenhum paciente encontrado para este filtro.').should('be.visible');
    });
  });

});
