# Auditoria de Privacidade, Proteção de Dados e Segurança — Sistema Lucas

> Documento de duas partes:
> **Parte I** — Auditoria de Conformidade LGPD (Lei nº 13.709/2018), sob a ótica de DPO.
> **Parte II** — Avaliação de Arquitetura de Segurança e Conformidade PCI-DSS, sob a ótica de CISO.

| Campo | Conteúdo |
|---|---|
| **Sistema auditado** | Sistema Lucas — Plataforma de Prontuário Eletrônico e Gestão Clínica |
| **Tecnologias principais** | Angular 21 (frontend) · Spring Boot 3.4 / Java 21 (backend) · PostgreSQL 15 · Docker Compose · nginx |
| **Natureza do tratamento** | Dados pessoais e **dados pessoais sensíveis de saúde** (LGPD Art. 5º, II) |
| **Gateway de pagamento** | **Inexistente** — o sistema não coleta, transmite ou armazena dados de cartão |
| **Metodologia** | Revisão de código-fonte (entidades JPA, serviços, controladores, segurança, configuração) |
| **Classificação geral de risco** | **ALTO** — tratamento de dados sensíveis com falhas críticas de gestão de chaves |

---
---

# PARTE I — Auditoria de Conformidade LGPD

## 1. Sumário Executivo

O Sistema Lucas é uma plataforma de prontuário eletrônico que, por definição, realiza tratamento de **dados pessoais sensíveis** relativos à saúde de pessoas naturais — categoria submetida ao regime reforçado do **Art. 11 da LGPD**. A auditoria identificou uma arquitetura de privacidade **parcialmente madura**: o sistema já adota cifragem de dados em repouso, hashing de senhas com Argon2id, registro demonstrável de consentimento, trilha de auditoria de acessos e anonimização irreversível compatível com a retenção obrigatória de prontuários.

Contudo, foram identificadas **12 não conformidades**, sendo **2 de severidade CRÍTICA** e **2 de severidade ALTA**, concentradas na **gestão de chaves criptográficas e segredos**. A principal recomendação estratégica é a **reclassificação da base legal do prontuário clínico**: o sistema atualmente trata o consentimento como base legal universal, quando os dados de saúde do prontuário devem repousar sobre o **Art. 11, II, "a"** (tutela da saúde por profissionais de saúde). Manter o consentimento como base do prontuário cria um **conflito jurídico insolúvel**: o titular poderia revogar o consentimento, mas a lei (Lei nº 13.787/2018) obriga a retenção do prontuário por 20 anos.

**Prioridades imediatas (30 dias):** substituição da chave de criptografia fraca, remoção do *fallback* de texto plano no conversor de cifra, e migração de segredos para cofre seguro.

---

## 2. Contexto do Sistema e Agentes de Tratamento

### 2.1 Papéis (LGPD Art. 5º, VI–IX)

| Papel | Agente | Função |
|---|---|---|
| **Controlador** | Instituto/Clínica operadora do Sistema Lucas | Define finalidades e meios do tratamento |
| **Operador** | Provedor de hospedagem (VPS) | Processa dados sob instrução do controlador |
| **Suboperador** | Google LLC (Gmail SMTP) | Transmite e-mails transacionais |
| **Encarregado (DPO)** | **NÃO DESIGNADO** — ver achado AUD-09 | Canal entre titulares, ANPD e controlador |

### 2.2 Perfis de usuário e visibilidade de dados

| Perfil | Acesso a dados |
|---|---|
| `ADMIN` | Gestão de usuários/profissionais; leitura da trilha de auditoria; exportações administrativas |
| `PROFESSIONAL` | Prontuários, documentos e agenda dos próprios atendimentos |
| `PATIENT` | Visualização/confirmação das próprias consultas e dos próprios documentos liberados |

### 2.3 Plataformas de terceiros integradas

A auditoria confirmou que o sistema é **majoritariamente autocontido** (monólito containerizado). A única integração externa de tratamento de dados é:

- **Google LLC — Gmail SMTP** (`smtp.gmail.com:587`): envio de e-mails de verificação de conta, redefinição de senha e lembretes de consulta. **Implica transferência internacional de dados** (ver Seção 6 e AUD-08).

Não foram identificadas integrações com gateways de pagamento, *analytics* de terceiros, Firebase, AWS ou ferramentas de *marketing*. **Nenhum dado financeiro/cartão de crédito é coletado.**

---

## 3. Mapeamento de Dados Pessoais (Registro das Operações de Tratamento — RoPA)

> Cumpre o requisito do **Art. 37 da LGPD** / **Art. 30 do RGPD**.

### 3.1 Dados de identificação e cadastro

| Dado | Titular | Sensível? | Estado no banco | Finalidade |
|---|---|---|---|---|
| Nome completo | Paciente/Profissional | Não | Texto plano | Identificação no atendimento |
| E-mail | Todos | Não | Texto plano | Autenticação e comunicação |
| Senha | Todos | Não (credencial) | Hash Argon2id | Autenticação |
| CPF | Paciente/Profissional | Não (mas crítico) | **Cifrado (AES-GCM)** + `cpf_hash` SHA-256 | Identificação inequívoca |
| Telefone | Paciente/Profissional | Não | **Cifrado (AES-GCM)** | Contato e lembretes |
| Endereço | Paciente/Profissional | Não | **Cifrado (AES-GCM)** | Cadastro clínico |
| Data de nascimento | Paciente/Profissional | Não | Texto plano | Identificação / faixa etária |
| Gênero | Paciente/Profissional | Pode ser¹ | Texto plano | Cadastro clínico |
| Contato de emergência (nome/telefone) | Paciente | Não | **Cifrado (AES-GCM)** | Proteção da vida do titular |
| Registro de conselho (CRM/CRP), especialidade | Profissional | Não | Texto plano | Habilitação profissional |

¹ *Gênero pode revelar dado sensível conforme o contexto (Art. 5º, II); tratar com minimização.*

### 3.2 Dados sensíveis de saúde (regime do Art. 11)

| Dado | Titular | Estado no banco | Finalidade |
|---|---|---|---|
| Alergias | Paciente | **Cifrado (AES-GCM)** | Segurança clínica do atendimento |
| Notas de prontuário | Paciente | **Cifrado (AES-GCM)** | Registro clínico do atendimento |
| Documentos médicos (texto e PDF em Base64) | Paciente | **Cifrado (AES-GCM)** | Laudos, receitas, exames |
| Motivo da consulta | Paciente | Texto plano² | Triagem do atendimento |

² *O campo `reason` do agendamento pode revelar condição de saúde — recomenda-se cifragem (ver AUD-11).*

### 3.3 Dados operacionais e de segurança

| Dado | Finalidade |
|---|---|
| Trilha de auditoria (`audit_logs`: e-mail, ação, entidade, data/hora) | Rastreabilidade de acesso a dados sensíveis |
| Tokens de verificação de e-mail e de redefinição de senha | Segurança da conta (uso único, curta validade) |
| Histórico de penalidades (`infractionCount`, `blockedUntil`) | Gestão de faltas/no-show |
| Registro de consentimento (`termsAccepted`, `termsAcceptedAt`, `termsVersion`) | Prova demonstrável do consentimento (Art. 8º, §1º) |

---

## 4. Bases Legais Aplicáveis (LGPD Arts. 7º e 11)

> **Princípio reitor:** cada operação de tratamento exige uma base legal específica. Para dados sensíveis, **não basta o consentimento** — o Art. 11 estabelece um rol próprio e mais restrito.

### 4.1 Matriz de bases legais

| Categoria de dado | Base legal recomendada | Fundamento |
|---|---|---|
| Conta de acesso (nome, e-mail, senha) | **Execução de contrato** | Art. 7º, V |
| CPF | **Execução de contrato** + **Cumprimento de obrigação legal** | Art. 7º, V e II |
| Telefone, endereço, data de nascimento | **Execução de contrato** | Art. 7º, V |
| Contato de emergência | **Proteção da vida/incolumidade física do titular** | Art. 7º, VII / Art. 11, II, "e" |
| **Alergias, notas de prontuário, documentos médicos, motivo da consulta** | **Tutela da saúde, em procedimento realizado por profissionais de saúde** | **Art. 11, II, "a"** |
| Registro profissional (CRM/CRP), especialidade | **Execução de contrato** + **Obrigação legal** | Art. 7º, V e II |
| Trilha de auditoria sobre dados de saúde | **Cumprimento de obrigação legal/regulatória** + **garantia da segurança** | Art. 11, II, "g"; Art. 7º, X |
| E-mails transacionais (verificação, redefinição, lembretes) | **Execução de contrato** | Art. 7º, V |
| Comunicações de marketing (caso venham a existir) | **Consentimento** (específico, destacado, revogável) | Art. 7º, I |

### 4.2 Achado estratégico — reclassificação da base legal do prontuário

> **AUD-05 (Severidade MÉDIA — impacto jurídico ALTO).** O fluxo de cadastro (`PatientService.create` / `AuthController.register`) condiciona **todo** o tratamento ao aceite dos Termos, tratando o **consentimento** como base legal universal.

**Problema:** o consentimento é **revogável a qualquer momento** (Art. 8º, §5º). Se o prontuário repousasse sobre consentimento, a revogação obrigaria a cessação do tratamento — porém a **Lei nº 13.787/2018** e a **Resolução CFM nº 1.821/2007** obrigam a guarda do prontuário por **20 anos**. Cria-se uma contradição insolúvel.

**Recomendação:** dissociar as bases legais:
- O **consentimento** (checkbox no cadastro) cobre legitimamente a **criação da conta** e o recebimento de **comunicações**.
- O **prontuário e os dados de saúde** devem ser declarados, na Política de Privacidade, como tratados com fundamento no **Art. 11, II, "a" (tutela da saúde)** — independente de consentimento e imune à sua revogação.

---

## 5. Direitos dos Titulares (LGPD Art. 18)

### 5.1 Situação atual verificada no código

| Direito | Implementação atual | Status |
|---|---|---|
| Confirmação e acesso (Art. 18, I–II) | `ExportService.exportPatientData` gera CSV de prontuários e documentos | ⚠️ Parcial |
| Correção (Art. 18, III) | `PatientService.updateMyProfile` — autoatendimento de perfil | ✅ Adequado |
| Portabilidade (Art. 18, V) | Exportação CSV existente | ⚠️ Parcial |
| Eliminação / direito ao esquecimento (Art. 18, VI) | `PatientService.deleteOrAnonymize` — exclusão física ou anonimização irreversível | ✅ Adequado |
| Informação sobre compartilhamento (Art. 18, VII) | Não há tela/documento que liste os terceiros | ❌ Ausente |
| Revogação do consentimento (Art. 18, IX) | Não há fluxo dedicado | ❌ Ausente |

### 5.2 Critérios de adequação

1. **Acesso e portabilidade — completude.** A exportação atual omite os **dados cadastrais do próprio titular** (nome, CPF, telefone, endereço, contato de emergência) e usa CSV com escape frágil. O titular tem direito à totalidade dos seus dados em **formato estruturado e interoperável** (Art. 18, V). Recomenda-se exportação em JSON estruturado, abrangendo todas as categorias da Seção 3.

2. **Eliminação — modelo híbrido correto.** A lógica `deleteOrAnonymize` está **juridicamente correta**: havendo vínculo clínico, executa **anonimização irreversível** dos dados de identificação (`isActive = false`), preservando o registro clínico exigido pela retenção legal. Recomenda-se apenas **documentar esse comportamento ao titular** no momento da solicitação.

3. **Revogação e prazo de resposta.** Implementar canal explícito de **revogação de consentimento** (para a parte revogável — conta e comunicações) e de exercício de direitos, com resposta em **até 15 dias** (Art. 19, II).

4. **Identidade e autenticidade.** Toda solicitação de exercício de direito deve ser autenticada (sessão JWT vigente) e **registrada na trilha de auditoria**.

---

## 6. Transparência e Compartilhamento com Terceiros

### 6.1 Inventário de compartilhamento

| Terceiro | Dados transmitidos | Papel | Localização | Transferência internacional |
|---|---|---|---|---|
| Provedor de VPS/hospedagem | Todos (em repouso) | Operador | A confirmar | A confirmar |
| Google LLC (Gmail SMTP) | E-mail, nome, conteúdo do e-mail transacional | Suboperador | Estados Unidos | **SIM (Art. 33)** |

### 6.2 Critérios para a cláusula de compartilhamento

A cláusula de compartilhamento da Política de Privacidade deve, obrigatoriamente:

- **Nomear** cada terceiro e descrever a finalidade específica do compartilhamento;
- Declarar que os terceiros atuam como **operadores/suboperadores**, vinculados por contrato e proibidos de uso para finalidade própria;
- **Informar a transferência internacional** para os Estados Unidos (envio de e-mails via Google), indicando o mecanismo de adequação (cláusulas-padrão contratuais — Art. 33, II);
- Afirmar que **dados sensíveis de saúde NÃO são compartilhados** para finalidades comerciais ou de *marketing*;
- Garantir contrato de operador (DPA) firmado com o provedor de hospedagem (Art. 39).

**Texto-modelo da cláusula:**

> *"O Sistema Lucas não comercializa nem cede seus dados pessoais. O compartilhamento ocorre exclusivamente com prestadores de serviço que atuam como operadores, sob contrato e instrução do Controlador: (i) provedor de infraestrutura de hospedagem, para armazenamento seguro dos dados; e (ii) Google LLC, exclusivamente para o envio de e-mails de verificação de conta, redefinição de senha e lembretes de consulta. O envio de e-mails implica transferência de dados de contato para servidores localizados nos Estados Unidos, amparada por cláusulas contratuais de proteção equivalentes às exigidas pela LGPD. Seus dados sensíveis de saúde — prontuários e documentos clínicos — jamais são compartilhados para fins comerciais ou publicitários."*

---

## 7. Retenção e Eliminação de Dados

### 7.1 Tabela de retenção recomendada

| Categoria de dado | Prazo de retenção | Critério legal | Destinação final |
|---|---|---|---|
| **Prontuário, documentos médicos, notas clínicas** | **Mínimo 20 anos** a contar do último registro | Lei nº 13.787/2018, Art. 6º; Res. CFM nº 1.821/2007 | Anonimização do titular; registro clínico preservado |
| Dados cadastrais (conta ativa) | Enquanto durar a relação clínica | Art. 15, I e III (LGPD) | Anonimização ou exclusão |
| Dados cadastrais após encerramento | Prazo prescricional aplicável (até 5 anos) | Art. 16, I | Exclusão definitiva |
| Trilha de auditoria de acesso a dados de saúde | Alinhar à retenção do prontuário (ou mínimo 5 anos) | Art. 11, II, "g"; dever de prestação de contas | Exclusão segura |
| Tokens de verificação / redefinição de senha | Expurgo imediato após uso ou expiração (≤ 24h) | Princípio da necessidade (Art. 6º, III) | Exclusão automática |
| Logs de acesso à aplicação | Mínimo 6 meses | Marco Civil da Internet, Art. 15 | Exclusão segura |
| Histórico de penalidades / no-show | Enquanto a conta estiver ativa | Legítimo interesse (gestão de agenda) | Zerado no desbloqueio |

### 7.2 Critérios e achados

- **Princípio da necessidade (Art. 6º, III) e do prazo determinado (Art. 15).** Nenhum dado deve ser retido além do prazo de sua finalidade.
- **Anonimização como término legítimo (Art. 5º, XI).** A anonimização já implementada é o mecanismo correto para encerrar o tratamento de PII **sem** violar a retenção obrigatória do prontuário.
- **Lacuna identificada (AUD-07):** não há **rotina automatizada de expurgo**. Tokens expirados, logs antigos e contas inativas permanecem indefinidamente — violação do princípio do prazo determinado.

---

## 8. Achados da Auditoria LGPD e Plano de Ação

| ID | Severidade | Achado | Recomendação |
|---|---|---|---|
| **AUD-01** | 🔴 **CRÍTICA** | Chave de criptografia (`ENCRYPTION_KEY`) fraca, previsível e legível por humanos; AES-128; armazenada em texto plano no `.env`. | Gerar chave aleatória de 256 bits (AES-256); migrar para cofre de segredos; rotacionar e recifrar a base. |
| **AUD-02** | 🔴 **CRÍTICA** | `EncryptionConverter` retorna o dado **bruto em texto plano** quando a descriptografia falha. | Remover o *fallback*; tratar falha como erro explícito e auditável. |
| **AUD-03** | 🟠 **ALTA** | `cpf_hash` usa SHA-256 puro do CPF — reversível por força bruta. | Substituir por **HMAC-SHA256** com segredo dedicado (*pepper*) fora do banco. |
| **AUD-04** | 🟠 **ALTA** | Segredos (JWT, banco, e-mail, chave de cifra, senha do admin) em texto plano em `.env`. | Migrar para Docker Secrets / cofre; restringir permissões; rotacionar. |
| **AUD-05** | 🟡 **MÉDIA** | Base legal do prontuário tratada como consentimento — conflita com a retenção obrigatória de 20 anos. | Reclassificar para Art. 11, II, "a" (ver Seção 4.2). |
| **AUD-06** | 🟡 **MÉDIA** | Exportação de dados do titular incompleta e em CSV não estruturado. | Exportação completa em JSON estruturado e interoperável. |
| **AUD-07** | 🟡 **MÉDIA** | Ausência de rotina automatizada de retenção/expurgo. | Implementar *job* de limpeza conforme a Seção 7.1. |
| **AUD-08** | 🟡 **MÉDIA** | Transferência internacional de dados (Google LLC/EUA) não informada ao titular. | Documentar na Política de Privacidade. |
| **AUD-09** | 🟡 **MÉDIA** | Encarregado (DPO) não designado nem publicado. | Designar e publicar nome e canal de contato (Art. 41). |
| **AUD-10** | 🟢 **BAIXA** | `ddl-auto=update` ativo em produção — risco de *drift* de schema. | Desativar `ddl-auto`; gerir schema só via Flyway. |
| **AUD-11** | 🟢 **BAIXA** | Unicidade de CPF carrega/descriptografa todos os pacientes em memória; `reason` da consulta não cifrado. | Usar `cpf_hash` na verificação; cifrar o motivo da consulta. |
| **AUD-12** | 🟢 **BAIXA** | Ausência de processo documentado de resposta a incidentes/comunicação à ANPD. | Elaborar plano de resposta a incidentes (Art. 48). |

### 8.1 Boas práticas já implementadas (pontos fortes)

- Cifragem de dados sensíveis em repouso (AES-GCM via `EncryptionConverter` e `@Convert`);
- Senhas protegidas com **Argon2id** (recomendado pelo OWASP);
- **Registro demonstrável de consentimento** com data/hora e versão (Art. 8º, §1º);
- **Trilha de auditoria** de acesso a dados sensíveis (`AuditLogService`);
- **Anonimização irreversível** conciliando o direito ao esquecimento com a retenção obrigatória;
- Verificação de e-mail, *rate limiting* e restrição de CORS.

---

## 9. Critérios Textuais — Termo de Consentimento (Cadastro)

O Termo apresentado no *checkbox* de cadastro deve observar os requisitos do **Art. 8º**:

1. **Destaque visual** — cláusula apartada, não pré-marcada.
2. **Linguagem clara** — acessível ao leigo.
3. **Especificidade** — informar finalidade, controlador e natureza clínica.
4. **Granularidade** — separar aceite obrigatório (uso da plataforma) de aceites opcionais (ex.: lembretes).
5. **Versionamento** — registrar a versão vigente (já implementado via `termsVersion`).

**Texto-modelo do aceite:**

> *"Li e concordo com os Termos de Uso e com a Política de Privacidade. Estou ciente de que o Sistema Lucas é uma plataforma de prontuário eletrônico que tratará meus dados pessoais e dados de saúde para a finalidade de prestação de cuidado clínico. Compreendo que posso, a qualquer momento, acessar, corrigir, exportar ou solicitar a eliminação dos meus dados, e revogar este consentimento para a manutenção da minha conta — ressalvada a guarda do prontuário pelo prazo legal de 20 anos."*

---

## 10. Critérios Textuais — Política de Privacidade

| Seção | Conteúdo obrigatório |
|---|---|
| 1. Identificação do Controlador | Razão social, CNPJ e endereço |
| 2. Encarregado (DPO) | Nome e canal de contato (Art. 41) |
| 3. Dados coletados | Categorias da Seção 3 |
| 4. Finalidades e bases legais | Matriz da Seção 4.1 |
| 5. Dados sensíveis de saúde | Declaração de tratamento sob o Art. 11, II, "a" |
| 6. Compartilhamento e terceiros | Cláusula-modelo da Seção 6.2 |
| 7. Transferência internacional | Envio de e-mails via Google LLC (EUA) |
| 8. Prazo de retenção | Tabela da Seção 7.1 (destaque para os 20 anos do prontuário) |
| 9. Direitos do titular | Lista do Art. 18 e como exercê-los |
| 10. Segurança | Cifragem, controle de acesso e trilha de auditoria |
| 11. Incidentes de segurança | Compromisso de comunicação ao titular e à ANPD (Art. 48) |
| 12. Atualizações | Política de versionamento e forma de notificação |

**Texto-modelo da seção de dados sensíveis:**

> *"O Sistema Lucas trata dados pessoais sensíveis relativos à sua saúde — incluindo histórico de alergias, anotações de prontuário e documentos clínicos. Esse tratamento é realizado por profissionais de saúde habilitados, com a finalidade exclusiva de tutela da sua saúde, com fundamento no Art. 11, inciso II, alínea 'a', da LGPD. Por se tratar de registro clínico de guarda obrigatória, o prontuário será mantido pelo prazo legal de 20 anos, ainda que você solicite o encerramento da sua conta — hipótese em que seus dados de identificação serão anonimizados de forma irreversível."*

---

## 11. Conclusão da Parte I — Roadmap de Conformidade LGPD

| Fase | Prazo | Ações |
|---|---|---|
| **Fase 1 — Contenção** | 30 dias | AUD-01, AUD-02, AUD-04 |
| **Fase 2 — Conformidade documental** | 60 dias | AUD-05, AUD-08, AUD-09 + publicação de Política e Termos |
| **Fase 3 — Direitos e ciclo de vida** | 90 dias | AUD-03, AUD-06, AUD-07, AUD-12 |
| **Fase 4 — Higiene técnica** | 120 dias | AUD-10, AUD-11 |

---
---

# PARTE II — Avaliação de Arquitetura de Segurança e Conformidade PCI-DSS

## 12. Escopo PCI-DSS e Premissa da Avaliação

### 12.1 Determinação de escopo — achado fundamental

> **O Sistema Lucas, na sua configuração auditada, está FORA DO ESCOPO do PCI-DSS.**

A revisão do código-fonte confirmou que **nenhum dado de titular de cartão** (PAN — número do cartão, CVV/CVC, dados de trilha magnética, PIN) é coletado, transmitido, processado ou armazenado. Não existe gateway de pagamento integrado, não há entidades JPA financeiras e não há fluxo de cobrança.

**Isto é uma força, não uma lacuna.** A estratégia de segurança correta para a clínica é **permanecer fora de escopo** — o menor escopo PCI-DSS é o escopo inexistente. Esta Parte II tem, portanto, duplo propósito:

- **(A) Avaliação real** dos controles de autenticação e acesso já existentes no sistema (Seção 14 e achados SEC-01 a SEC-04 e SEC-06) — aplicáveis **hoje**, independentemente de pagamentos, por serem o perímetro que protege os dados sensíveis de saúde.
- **(B) Blueprint de arquitetura obrigatório** a ser cumprido **caso e quando** uma funcionalidade de cobrança (ex.: cobrança de consultas ou mensalidade recorrente) venha a ser introduzida (Seções 13, 15 e achado SEC-05).

### 12.2 Cenário financeiro hipotético considerado no blueprint

Para fins do desenho de arquitetura da Seção 13 e 15, assume-se o cenário mais sensível e provável para uma clínica: **cobrança de consultas com possibilidade de recorrência mensal e retentativas automáticas**, intermediada por um **gateway de pagamento PCI-validado** (ex.: Stripe, ASAAS, Mercado Pago ou Pagar.me — todos com checkout hospedado e tokenização).

---

## 13. Armazenamento de Dados Sensíveis — Princípio "Zero Storage"

### 13.1 Regra inegociável (PCI-DSS Requisito 3)

| Dado | Pode armazenar? | Observação |
|---|---|---|
| PAN (número completo do cartão) | ❌ **Evitar totalmente** | Se inevitável, exige cifragem forte e escopo PCI completo |
| CVV / CVC / CID | ❌ **NUNCA** — proibição absoluta | Proibido mesmo cifrado, mesmo temporariamente (Req. 3.3.1) |
| Dados de trilha magnética / chip | ❌ **NUNCA** | Proibido após autorização |
| PIN / bloco de PIN | ❌ **NUNCA** | — |
| Token opaco do gateway | ✅ Sim | Referência substituta, sem valor fora do gateway |
| Bandeira do cartão (Visa, Master…) | ✅ Sim | Dado não sensível |
| Últimos 4 dígitos do PAN | ✅ Sim | Permitido para exibição/identificação |
| Mês/ano de validade | ✅ Sim (se necessário) | Apenas se a regra de negócio exigir |

### 13.2 Critérios de arquitetura para o "Zero Storage"

1. **Tokenização obrigatória.** Os dados do cartão são capturados **diretamente pelo gateway** e nunca trafegam pelo backend Spring Boot. O gateway retorna um **token opaco**; o sistema persiste somente esse token + bandeira + últimos 4 dígitos.

2. **Permanecer no SAQ A (menor escopo de autoavaliação).** Utilizar **checkout hospedado** (redirecionamento para a página do gateway) ou **campos hospedados** (*hosted fields* / *iframe* servido pelo gateway). Em ambos, o dado do cartão **nunca toca o DOM da aplicação Angular nem o servidor**.

3. **Isolamento de origem.** A captura de cartão jamais deve ocorrer em um formulário Angular nativo. Nenhum componente da aplicação deve ter acesso, sequer transitório, ao valor do PAN ou do CVV.

4. **Modelo de dados de pagamento — desenho de referência.** A entidade financeira, quando criada, deve conter **exclusivamente** campos não sensíveis:

   ```
   PaymentMethod
   ├── id
   ├── patient_id        (FK)
   ├── gateway_token     (token opaco — referência no gateway)
   ├── card_brand        (ex.: "VISA")
   ├── card_last4        (ex.: "4242")
   ├── exp_month / exp_year
   └── created_at
   ```

   Não existe coluna para PAN, CVV ou dados de trilha. **A ausência dessas colunas é um controle de segurança por design.**

5. **Logs e telemetria.** PAN e CVV nunca devem aparecer em logs de aplicação, mensagens de erro, *stack traces* ou ferramentas de APM. Configurar mascaramento na camada de log.

6. **Transmissão.** Todo tráfego com o gateway sob TLS 1.2+ (Requisito 4). O nginx do projeto já termina TLS — manter *ciphers* fortes e HSTS.

---

## 14. Autenticação e Controle de Acessos (avaliação do estado atual)

### 14.1 Estado atual verificado no código

| Controle | Situação atual | Avaliação |
|---|---|---|
| **RBAC** | Três papéis (`ADMIN`, `PROFESSIONAL`, `PATIENT`) via `@EnableMethodSecurity` e `Role` no JWT | ✅ Implementado e adequado |
| **Hashing de senha** | Argon2id (`Argon2PasswordEncoder(16,32,2,65536,3)`) | ✅ Forte |
| **MFA** | Inexistente | ❌ **Lacuna crítica para o painel ADMIN** |
| **Token de sessão** | JWT HMAC-SHA256, validade fixa de **2 horas**, sem *refresh token* | ⚠️ Sem renovação nem revogação |
| **Armazenamento do token (frontend)** | `localStorage` (`auth.service.ts`) | ❌ **Vulnerável a XSS** |
| **Logout** | Apenas remove o token do `localStorage` — o token continua válido no servidor até expirar | ⚠️ Sem invalidação no servidor |
| **Rate limiting** | Bucket4j, 30 req/min por IP, **somente** em `/auth/*`, em memória por instância | ⚠️ Cobertura e robustez parciais |

### 14.2 Critérios de arquitetura recomendados

1. **MFA para acesso administrativo (PCI-DSS Req. 8.4 / 8.5).** O perfil `ADMIN` acessa **todos os dados sensíveis de saúde** e a trilha de auditoria — é a credencial de maior valor do sistema. MFA (TOTP) deve ser **obrigatório** para o login `ADMIN` e recomendado para `PROFESSIONAL`.

2. **Endurecimento do JWT.**
   - Reduzir o *access token* para **15 minutos**;
   - Introduzir *refresh token* rotativo, de uso único, armazenado de forma segura;
   - Manter uma lista de revogação (*denylist*) para permitir *logout* efetivo e resposta a incidentes.

3. **Armazenamento do token.** Migrar de `localStorage` para **cookie `HttpOnly` + `Secure` + `SameSite=Strict`**, eliminando a exposição a roubo de token via XSS. Caso a arquitetura exija o token acessível ao JS, aplicar **Content-Security-Policy** rígida como mitigação compensatória.

4. **Gestão de sessão administrativa (PCI-DSS Req. 8.2.8).** *Timeout* por inatividade de **15 minutos** para sessões `ADMIN`.

5. **Princípio do menor privilégio (PCI-DSS Req. 7).** Revisar periodicamente se cada *endpoint* exige o papel mínimo necessário. Toda ação administrativa sensível deve gerar registro em `audit_logs`.

6. **Rate limiting abrangente.** Estender o *rate limiting* para *endpoints* sensíveis fora de `/auth/*` (exportação de dados, prontuário, documentos) e, em ambiente multi-instância, externalizar o contador (ex.: Redis) para que o limite seja global e sobreviva a reinícios.

---

## 15. Mitigação de Riscos de Fraude no Gateway (blueprint)

> Aplicável **quando** a funcionalidade de cobrança for introduzida. Todas as verificações abaixo são executadas **pelo gateway**, nunca pelo backend.

### 15.1 Verificações no momento da cobrança

| Controle | Função | Efeito |
|---|---|---|
| **3-D Secure 2.0 (EMV 3DS)** | Autenticação do portador junto ao banco emissor | Desloca a responsabilidade do *chargeback* fraudulento para o emissor (*liability shift*) |
| **Validação de CVC/CVV** | Confirma posse física do cartão | Bloqueia uso de PAN vazado sem o cartão |
| **AVS (Address Verification System)** | Confere o endereço informado com o do emissor | Sinaliza transações suspeitas |
| **Verificação de valor/limite** | Rejeita valores fora do padrão do negócio | Reduz teste de cartões (*card testing*) |
| **Velocity checks** | Limita tentativas por cartão/IP/intervalo | Mitiga ataques automatizados |

### 15.2 Segurança de Webhooks (crítico)

O *status* financeiro **nunca** deve ser confiado a partir do redirecionamento de retorno no navegador do cliente — esse caminho é manipulável. A fonte da verdade é o **webhook assinado** enviado servidor-a-servidor pelo gateway.

- **Verificação de assinatura.** Todo webhook deve ter sua assinatura HMAC validada com o segredo do gateway **antes** de qualquer processamento. Eventos sem assinatura válida são descartados.
- **Idempotência.** Cada evento carrega um identificador único; processá-lo mais de uma vez não pode gerar cobrança/crédito duplicado. Persistir os IDs já processados.
- **Reconciliação assíncrona.** O pedido só é marcado como pago após o webhook `payment.succeeded` autêntico — não após o redirecionamento do usuário.
- **Endpoint dedicado e restrito.** A rota de webhook fica fora da autenticação JWT (é chamada pelo gateway), mas protegida pela verificação de assinatura e, idealmente, por *allowlist* de IPs do gateway.

### 15.3 Cobrança recorrente e contestações (*disputes*)

- **Retentativas com *backoff*.** Em falha de cobrança recorrente, aplicar retentativas com intervalo crescente e **limite máximo** de tentativas, seguido de fluxo de *dunning* (notificação ao paciente).
- **Tratamento de *chargeback*.** Reagir aos eventos de disputa do webhook (`charge.dispute.created`), suspender o serviço quando aplicável e preservar evidências (comprovante de consentimento, *logs* de atendimento) para a contestação.
- **Trilha de auditoria financeira.** Toda transação, retentativa e disputa registrada de forma imutável — reaproveitar o padrão do `AuditLogService` já existente.

---

## 16. Achados de Segurança e Plano de Ação

| ID | Severidade | Achado | Recomendação |
|---|---|---|---|
| **SEC-01** | 🟠 **ALTA** | JWT armazenado em `localStorage` — exposto a roubo de sessão via XSS. | Migrar para cookie `HttpOnly`+`Secure`+`SameSite=Strict`; aplicar CSP rígida. |
| **SEC-02** | ~~🟠 **ALTA**~~ | ~~Ausência de MFA no painel administrativo.~~ | **Dispensado** — decisão da instituição (2026-05-21). Colunas `mfa_enabled`/`totp_secret` existem no banco para implementação futura se necessário. |
| **SEC-03** | 🟡 **MÉDIA** | JWT de 2h sem *refresh token* nem revogação; *logout* não invalida o token no servidor. | *Access token* curto (15 min) + *refresh* rotativo + *denylist* de revogação. |
| **SEC-04** | 🟡 **MÉDIA** | *Rate limiting* só em `/auth/*`, em memória e por instância. | Estender a *endpoints* sensíveis; externalizar o contador (Redis) em multi-instância. |
| **SEC-05** | 🟡 **MÉDIA** (condicional) | Blueprint de pagamento (Zero Storage, 3DS, webhooks assinados) ainda não implementado. | Pré-requisito obrigatório **antes** de qualquer integração de cobrança (Seções 13 e 15). |
| **SEC-06** | 🟢 **BAIXA** | *Timeout* de sessão administrativa por inatividade inexistente. | Aplicar *timeout* de 15 min para sessões `ADMIN` (PCI-DSS Req. 8.2.8). |

---

## 17. Próxima Tarefa Atômica (Baby Step)

> Definição do **próximo passo técnico imediato** para a equipe de engenharia, isolado, reversível e não disruptivo.

**Tarefa:** *Preparar a fundação de dados para o MFA administrativo (achado SEC-02), sem ainda alterar o fluxo de login.*

**Justificativa da escolha:** entre todos os achados, SEC-02 (MFA do `ADMIN`) é o de **maior retorno de segurança com menor superfície de risco** — protege a credencial que enxerga 100% dos dados sensíveis de saúde, é também um requisito PCI-DSS (Req. 8.4) e independe da existência de pagamentos. O *baby step* abaixo é puramente aditivo: não toca o `SecurityFilter` nem o `AuthController`, portanto **não pode quebrar o login atual**.

**Escopo exato do passo:**

1. Criar a migração Flyway `V10__add_mfa_columns.sql` adicionando à tabela `users`:
   - `mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE`
   - `totp_secret VARCHAR(255)` — armazenado **cifrado** via `EncryptionConverter`, nulo enquanto o MFA não for ativado.
2. Adicionar os campos correspondentes na entidade `User.java` (`mfaEnabled`, `totpSecret` com `@Convert`).
3. **Não** alterar `generateToken`, `SecurityFilter` nem o fluxo de `/auth/login` nesta etapa.

**Critério de pronto (*Definition of Done*):** a aplicação sobe, a migração V10 é aplicada com sucesso, o login existente permanece inalterado e as novas colunas existem vazias — prontas para a etapa seguinte (endpoint de *enrollment* TOTP e verificação no login).

**Por que parar aqui:** o passo entrega uma fundação testável e versionada sem risco de regressão. As etapas subsequentes — geração de *secret* TOTP, tela de *enrollment*, segundo fator no login — partem desta base já consolidada.

---

> *Documento elaborado sob a ótica de Encarregado de Proteção de Dados (DPO — Parte I) e de Arquiteto de Segurança da Informação (CISO — Parte II). Não constitui parecer jurídico vinculante; recomenda-se validação pelo departamento jurídico do Controlador antes da publicação dos instrumentos.*
