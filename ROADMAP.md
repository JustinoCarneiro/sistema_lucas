# Sistema Lucas — Blueprint Técnico (ROADMAP)

> Reconstruído a partir do código real em 29/07/2026 (projeto já em Fase 4/5). Pesos refletem a
> complexidade observada no código já construído, não uma estimativa prévia. Rastreabilidade
> história↔módulo é N:1 conforme o padrão Onda-Dev.

## Módulos

> Coluna **Status** segue a convenção da seção 12 da metodologia — é o quadro Kanban do projeto.
> Todos marcados `✅ Concluído (retroativo)` porque o sistema já estava inteiro em produção quando
> este ROADMAP foi reconstruído (29/07/2026) — não houve acompanhamento módulo-a-módulo ao vivo.

| # | Módulo | Peso | Épico(s) | Status | Observação |
|---|---|---|---|---|---|
| M1 | Autenticação & Sessão | **Grande · risco alto** | E1 | ✅ Concluído (retroativo) | JWT+refresh rotativo+denylist, sem `AuthService` dedicado (lógica no controller) |
| M2 | Gestão de Perfil | Pequeno | E2 | ✅ Concluído (retroativo) | CRUD simples, exclusão com guarda de FK |
| M3 | Disponibilidade | Médio | E3 | ✅ Concluído (retroativo) | Regras de janela de edição + proteção contra remover slot ocupado |
| M4 | Agendamento (máquina de estados) | **Grande · risco alto** | E4 | ✅ Concluído (retroativo) | 7 estados, 3 atores, guardas de transição em texto livre no service |
| M5 | Prontuário Eletrônico | Médio · risco alto pela sensibilidade | E5 | ✅ Concluído (retroativo) | Ponto único que conclui consulta |
| M6 | Documentos Clínicos | Médio | E6 | ✅ Concluído (retroativo) | Validação de magic bytes + limite de tamanho |
| M7 | Penalidades | Médio | E7 | ✅ Concluído (retroativo) | Máquina de estado própria (advertência→bloqueio→reset) |
| M8 | Exportação & Portabilidade | Pequeno | E8 | ✅ Concluído (retroativo) | CSV (admin/profissional) + JSON (paciente) |
| M9 | Painel Administrativo | Médio | E9 | ✅ Concluído (retroativo) | 3 dashboards, sem Service dedicado (acesso direto a repositórios) |
| M10 | Segurança & Conformidade LGPD | **Grande · risco alto · transversal** | E10 | ✅ Concluído (retroativo) | Criptografia de campo, hash de CPF, anonimização, auditoria |

**Coração do sistema:** M4 (Agendamento) é o módulo de maior risco — máquina de estados com 3 atores diferentes mexendo no mesmo recurso, guardas de transição não centralizadas (cada método do `AppointmentService` valida seu próprio pré-requisito), e efeito colateral cruzado com M5 (só o `ProntuarioService` conclui a consulta) e M7 (penalidade disparada a partir de M4).

## Contratos de API (Request/Response) — endpoints representativos por módulo

> Convenção real observada (ver ressalva de `/api/v1` no `CLAUDE.md`): rotas sem prefixo comum, direto na raiz. Documentado como está, não como deveria ser.

### M1 · Autenticação

**POST `/auth/login`**
```
Request:  { "email": string, "password": string }
Response: 200 — cookies HttpOnly "token" (JWT, 15min) + "refresh_token" (7 dias), sem corpo com dado sensível
          401 — credenciais inválidas
```

**POST `/auth/register`**
```
Request:  { "name": string, "email": string, "password": string, "cpf": string,
            "termsAccepted": boolean, "termsVersion": string, ... }
Response: 201 — usuário criado, role sempre PATIENT (não é campo do request)
          409 — e-mail ou CPF já cadastrado (checagem cross-role)
```

**POST `/auth/refresh`**
```
Request:  cookie "refresh_token"
Response: 200 — novo par de cookies (token + refresh_token), o antigo refresh é invalidado
          401 — refresh token inválido, expirado ou já usado
```

### M3 · Disponibilidade

**POST `/disponibilidade/mensal?mes=yyyy-MM`**
```
Request:  { "slots": [ { "date": "yyyy-MM-dd", "startTime": "HH:mm" }, ... ] }
Response: 200 — grade do mês substituída
          400 — mês passado, ou slot removido tem consulta ativa vinculada
```

**GET `/disponibilidade/{profissionalId}/slots?data=yyyy-MM-dd`**
```
Response: 200 — [ "HH:mm", ... ]  (exclui ocupados e horários já passados, fuso America/Sao_Paulo)
```

### M4 · Agendamento

**POST `/consultas`**
```
Request:  { "professionalId": UUID, "dateTime": ISO-8601 }
Response: 201 — consulta criada em AGUARDANDO_CONFIRMACAO
          400 — paciente bloqueado por penalidade (inclui data de liberação)
          409 — slot já ocupado
```

**PATCH `/consultas/{id}/aprovar`** · **PATCH `/consultas/{id}/recusar`**
```
Request (recusar): { "justificativa": string }
Response: 200 — estado atualizado (AGENDADA ou CANCELADA)
          400 — consulta não está mais em AGUARDANDO_CONFIRMACAO
```

**PATCH `/consultas/{id}/confirmar-profissional`** · **PATCH `/consultas/{id}/confirmar-paciente`**
```
Response: 200 — estado avança (CONFIRMADA_PROFISSIONAL ou CONFIRMADA)
          400 — fora de ordem (paciente tentando confirmar antes do profissional) ou já confirmada
```

**POST `/consultas/{id}/cancelar`**
```
Request:  { "justificativa": string }
Response: 200 — CANCELADA. Penalidade aplicada se <24h e estado já passou de AGUARDANDO_CONFIRMACAO
          400 — justificativa ausente
```

### M5 · Prontuário

**POST `/prontuarios`**
```
Request:  { "appointmentId": UUID, "notas": string }
Response: 201 — prontuário criado, consulta associada marcada CONCLUIDA (efeito colateral, não é
          parâmetro do request)
```

### M6 · Documentos

**POST `/documentos`**
```
Request:  { "pacienteId": UUID, "tipo": enum, "titulo": string,
            "conteudoTexto"?: string, "arquivoBase64"?: string, "nomeArquivo"?: string }
Response: 201 — criado com disponivel=false
          400 — PDF sem magic bytes válidos, ou acima do limite de tamanho (~5MB)
```

### M8 · Exportação

**GET `/export/patient`**
```
Response: 200 — application/json — dados cadastrais + prontuários + documentos + consultas +
          metadados de consentimento LGPD (não é CSV, diferente dos exports de admin/profissional)
```

### M10 · Segurança (não é endpoint, é comportamento transversal)

**Toda entidade com campo sensível** (`Patient`, `Professional`, `Appointment`, `Documento`, `Prontuario`) aplica `EncryptionConverter` (`@Convert`) nos campos listados no `docs/spec.md` (ÉPICO 10) — isso não aparece no contrato JSON do endpoint (a serialização do DTO já entrega o valor decifrado), mas é o motivo pelo qual `EncryptionConverter` precisa ser considerado em qualquer endpoint novo que exponha esses campos.
