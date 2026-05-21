# Plano de Resposta a Incidentes de Segurança — Sistema Lucas

> Documento exigido pela auditoria LGPD (achado **AUD-12**) — atende ao dever de
> comunicação de incidentes do **Art. 48 da Lei nº 13.709/2018**.
> Campos entre `[ ]` devem ser preenchidos pelo Controlador antes da publicação.

| Campo | Conteúdo |
|---|---|
| **Controlador** | `[Razão social / CNPJ da clínica]` |
| **Sistema** | Sistema Lucas — Prontuário Eletrônico e Gestão Clínica |
| **Encarregado (DPO)** | Ver `nota_designacao_dpo.md` |
| **Versão** | 1.0 |
| **Última revisão** | `[data]` |

---

## 1. Objetivo e Escopo

Estabelecer o procedimento de detecção, contenção, avaliação, comunicação e
recuperação de **incidentes de segurança com dados pessoais** tratados pelo
Sistema Lucas — incluindo **dados sensíveis de saúde**. Aplica-se a toda a
equipe técnica, administrativa e aos profissionais de saúde usuários.

## 2. Definição de Incidente de Segurança

Qualquer evento, confirmado ou suspeito, que comprometa a **confidencialidade,
integridade ou disponibilidade** de dados pessoais. Exemplos:

- Acesso não autorizado a prontuários, documentos clínicos ou dados cadastrais;
- Vazamento, cópia ou exfiltração de dados (interna ou externa);
- Perda de dispositivo ou credencial com acesso ao sistema;
- Comprometimento de chave de criptografia, segredo (`JWT_SECRET`, `ENCRYPTION_KEY`) ou senha administrativa;
- Ataque (ransomware, SQL injection, brute-force, XSS) bem-sucedido ou em andamento;
- Indisponibilidade prolongada por falha ou ataque (DoS);
- Erro operacional que exponha dados (ex.: exportação enviada ao destinatário errado).

## 3. Equipe de Resposta e Papéis

| Papel | Responsável | Atribuição |
|---|---|---|
| **Coordenador do incidente** | `[nome/cargo]` | Conduz a resposta e decide a comunicação |
| **Encarregado (DPO)** | `[nome]` | Avalia risco aos titulares; comunica ANPD e titulares |
| **Responsável técnico** | `[nome]` | Contenção, erradicação e recuperação técnica |
| **Responsável jurídico** | `[nome]` | Apoia a avaliação legal e a comunicação |

Canal interno de notificação de incidentes: `[e-mail/telefone de plantão]`.

## 4. Fluxo de Resposta

### 4.1 Detecção e registro (imediato)
Qualquer pessoa que identifique um incidente deve comunicar o Coordenador
**imediatamente**. Registrar: data/hora da detecção, quem detectou, descrição,
sistemas e dados potencialmente afetados. Consultar a trilha de auditoria
(`audit_logs`) e os logs da aplicação.

### 4.2 Contenção (até 24 h)
- Isolar o vetor: revogar credenciais/tokens comprometidos (denylist de JWT,
  revogação de refresh tokens), bloquear IPs, suspender contas afetadas;
- Em caso de comprometimento de chave/segredo: **rotacionar** `JWT_SECRET`,
  `ENCRYPTION_KEY` e/ou senhas, e re-cifrar os dados (ver `EncryptionMigrationRunner`);
- Preservar evidências (logs, *snapshots*, backups) antes de qualquer alteração.

### 4.3 Erradicação e recuperação
- Eliminar a causa raiz (corrigir vulnerabilidade, remover acesso indevido);
- Restaurar a partir de backup íntegro (`backup.sh`) quando necessário;
- Validar a integridade dos dados antes de retornar o serviço.

### 4.4 Avaliação de risco aos titulares
O Encarregado avalia, com base em:
- Categorias de dados afetados (dado de saúde = **alto risco**);
- Volume de titulares atingidos;
- Possibilidade de identificação e de dano (discriminação, fraude, exposição clínica);
- Se os dados estavam **cifrados** e se a chave foi comprometida.

### 4.5 Comunicação (Art. 48 da LGPD)
Havendo **risco ou dano relevante** aos titulares, o Encarregado comunica:

- **À ANPD** — em prazo razoável (a Autoridade orienta até 3 dias úteis),
  informando: natureza dos dados, titulares envolvidos, medidas técnicas de
  proteção, riscos e medidas adotadas/propostas;
- **Aos titulares afetados** — descrição do ocorrido, dados envolvidos, riscos
  e recomendações de proteção, em linguagem clara.

Modelo de comunicação ao titular e à ANPD: `[anexar / link]`.

## 5. Registro e Pós-Incidente

- Todo incidente é registrado em um **livro de incidentes** (data, classificação,
  ações, comunicação realizada, encerramento);
- Reunião de lições aprendidas em até 15 dias após o encerramento;
- Atualização deste plano e dos controles técnicos conforme as falhas identificadas.

## 6. Prevenção (controles já existentes no Sistema Lucas)

- Criptografia de dados sensíveis em repouso (AES-256 GCM);
- Senhas com Argon2id; sessão por cookie `HttpOnly`; *denylist* de tokens;
- Trilha de auditoria de acessos (`AuditLogService`);
- *Rate limiting* contra força bruta; expurgo automático de tokens (`DataCleanupTask`);
- Backups periódicos (`backup.sh`).

---

> *Documento interno. Deve ser revisado pelo menos anualmente e validado pelo
> responsável jurídico do Controlador.*
