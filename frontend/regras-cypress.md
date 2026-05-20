# Diretrizes de Testes Cypress (E2E) - Projeto Lucas

Ao criar, atualizar ou refatorar testes Cypress neste projeto Angular, o assistente DEVE seguir rigorosamente estas regras e NUNCA alterar a sintaxe ou o padrão arquitetural:

1. **Nomenclatura e Estrutura:**
   - Os ficheiros devem ser criados na diretoria `cypress/e2e/`.
   - Utilizar o formato `[numero]-[nome-do-fluxo].cy.ts` (ex: `02-dashboard.cy.ts`).
   - Usar TypeScript (`/// <reference types="cypress" />`).

2. **Autenticação, Perfis (RBAC) e Sessão:**
   - O sistema possui 3 perfis distintos (ADMIN, PROFESSIONAL, PATIENT), o que altera drasticamente a renderização (ex: Dashboard).
   - Divida os testes no `.cy.ts` em contextos (`context('Visão do Admin', () => {...})`).
   - NUNCA reproduza os passos manuais de login via interface nos testes que não sejam de Auth. O comando customizado `cy.login(email, password)` DEVE ser sempre programático (via `cy.request` injetando o JWT no `localStorage`) para evitar lentidão e timeouts.

3. **Mapeamento de Seletores (Angular-friendly):**
   - Como o sistema não utiliza amplamente atributos `data-cy`, a IA deve basear-se estritamente nestes seletores na seguinte ordem de prioridade:
     1. Formulários: Atributos Reativos `cy.get('input[formControlName="nome_do_campo"]')` ou `id` (ex: `input#email`).
     2. Ações: Textos exatos dos botões usando `cy.contains('button', 'Texto do Botão')`.
     3. Validações/Alertas: Classes Tailwind estritas (ex: `div[class*="bg-red-50"]`).
   - O sistema usa Angular Signals (`()`); confie na retentativa automática do Cypress (built-in retryability) usando `.should('be.visible')` em vez de usar `cy.wait()` com tempos fixos.

4. **Isolamento de Estado e Testes Híbridos (Mocks vs Real):**
   - Para cenários focados na UI, utilize `cy.intercept()` fornecendo um alias (`.as('rotaMockada')`) e retornando fixtures (JSONs) compatíveis.
   - Sempre utilize `cy.wait('@rotaMockada')` antes de asserir os elementos na tela para garantir que os Signals atualizaram a view.
   - Para cenários que precisam testar o backend real integrado à UI, use `cy.request()` no `beforeEach` para o setup ou teardown da massa de dados no backend, mas NUNCA faça *hardcode* de URLs como `http://localhost:8081`. Utilize sempre variáveis de ambiente (ex: `Cypress.env('apiUrl')` ou urls relativas caso configuradas no Cypress).

5. **Gestão de Rate Limiters:**
   - Nos raros testes end-to-end reais (sem mock) feitos via interface, programe os cenários negativos (erros de validação, senhas incorretas) PRIMEIRO, antes de submeter formulários corretos, para evitar bloqueios da API.

6. **Protocolo de Autocorreção Obrigatória:**
   - Após a geração do script de teste, você DEVE executar autonomamente no terminal: `npx cypress run --spec <caminho-do-ficheiro>`.
   - Caso existam falhas (timeout, elemento não encontrado devido a seletores incorretos), analise o log de erro, ajuste o mapeamento no `.cy.ts` e rode novamente o teste até obter 100% de sucesso.