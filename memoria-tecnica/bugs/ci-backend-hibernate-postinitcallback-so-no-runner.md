---
tipo: bug
data: 2026-08-27
severidade: Média
status: Aberto — não corrigido, fora do escopo da validação de 27/08/2026
---

# CI do backend falha só no runner do GitHub Actions — Hibernate `PostInitCallback queue could not be processed`

## Sintoma
Ao corrigir `ci-backend.yml` (java-version 17→21, remoção do profile `-P ci` inexistente — ver
commit `fd6d25f`) e disparar o CI de verdade pela primeira vez em meses, todo teste anotado com
`@SpringBootTest` falha (`LucasApplicationTests`, `AuthControllerTest`, `PatientControllerTest`,
`AppointmentControllerTest`, `DashboardControllerTest`, `ProfessionalControllerTest`) — 25 testes
no total. Testes que só usam `@ExtendWith(MockitoExtension.class)` (a grande maioria da suíte,
service tests) passam normal — só quem sobe o contexto Spring completo falha.

**Não reproduz localmente** — `./mvnw verify` local, mesmo commit exato, passa 100% (188/188).

## Causa raiz (parcial — não totalmente investigada)
```
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build
Hibernate SessionFactory; nested exception is java.lang.IllegalStateException: PostInitCallback
queue could not be processed...
    - PostInitCallbackEntry - Entity(com.sistema.lucas.model.User) `sqmMultiTableMutationStrategy` interpretation
    - PostInitCallbackEntry - Entity(com.sistema.lucas.model.User) `sqmMultiTableInsertStrategy` interpretation
    - PostInitCallbackEntry - Entity(com.sistema.lucas.model.Patient) `sqmMultiTableInsertStrategy` interpretation
    - PostInitCallbackEntry - Entity(com.sistema.lucas.model.Professional) `sqmMultiTableInsertStrategy` interpretation
```
Acontece na inicialização do `HibernateJpaConfiguration` — a interpretação SQM (Semantic Query
Model) das entidades com herança `JOINED` (`User` → `Patient`/`Professional`) não termina de
processar antes do contexto seguir adiante. Hipótese mais provável: um bug de concorrência/timing
do Hibernate ORM 6.6.8 relacionado à inicialização lazy do cache de interpretação SQM sob
condições de CPU/thread diferentes das da máquina local (runner do GH Actions tem menos núcleos e
características diferentes) — não confirmado com certeza, não tive tempo de isolar (trocar versão
do Hibernate, testar com `-Xss`/threads diferentes, etc.).

## Por que não foi corrigido agora
Descoberto no meio de uma tarefa de validação de 3 entregas (E4/US-4.8, E11, MFA) que não tem
relação nenhuma com este bug — nenhuma delas mexeu em `User`/`Patient`/`Professional` ou na
config do Hibernate. Isolar a causa raiz exigiria iteração (cada tentativa = 1 push + esperar o
runner, ~1min cada) sem garantia de quantas tentativas seriam necessárias — fora de escopo pra
não travar a validação em andamento.

**O que isso NÃO significa:** não há evidência nenhuma de que os testes em si estejam errados —
é um problema do ambiente de execução do CI, não do código de produção nem da suíte de teste.
A suíte roda 100% localmente, o que já foi a validação usada em toda esta sessão.

## Atualização 01/09/2026
Ainda reproduz em todo push no `main` (Backend CI vermelho desde 28/08; Frontend CI vermelho
desde 30/07 — o job Cypress "Start containers" cai pelo mesmo motivo, o `lucas-api` não sobe no
runner). Contagem de erros subiu de 25 → **38** só porque mais testes `@SpringBootTest` foram
adicionados desde a nota original — mesma causa. Verificado no PR #1 (fix E7): os 3 checks
ficaram idênticos ao `main` (Backend JUnit ❌, Cypress ❌, Vitest ✅) — **o PR não introduziu
falha nova**; a suíte de backend roda 213/213 verde localmente, incluindo os testes de contexto.
Como a equipe faz deploy por `push-and-deploy.sh` (rsync, não depende do CI), o vermelho não
trava entrega — mas mascara regressão real de qualquer PR futuro.

## Candidato de correção mais provável (não testado)
`PostInitCallback queue could not be processed` + `sqmMultiTableMutationStrategy` em herança
`JOINED` é um bug conhecido do Hibernate ORM 6.6.x, corrigido em patches posteriores ao 6.6.8
(que o Spring Boot 3.4.3 traz). **Tentar primeiro:** override de uma linha no `pom.xml`
(`<properties>`): `<hibernate.version>6.6.13.Final</hibernate.version>` (ou o patch 6.6.x mais
recente compatível), e observar o runner. É um patch de mesma minor, risco baixo, mas precisa de
1 PR pra validar no CI (não reproduz local). Tarefa focada de ~30 min, não de wrap-up.

## Próximo passo (quando alguém tiver tempo pra investigar)
1. Aplicar o candidato acima (bump de `<hibernate.version>`) num PR dedicado e ver o runner.
2. Se não resolver: reproduzir localmente limitando CPU/threads (`docker run --cpus=1` rodando o
   build, ou `-Dmaven.test.jvm.args` com poucos threads) pra confirmar se é concorrência.
3. Buscar issues abertas no Hibernate ORM sobre `PostInitCallback` + `sqmMultiTableMutationStrategy`.

## Ligado a
- Achado durante a validação de homologação das 3 entregas de 27/08/2026 (E4/US-4.8, E11, MFA).
