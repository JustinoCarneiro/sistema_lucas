---
tipo: bug
data: 2026-08-27
severidade: Média
status: Resolvido
---

# `AppointmentConcurrencyIntegrationTest` nunca testava a trava real (índice único parcial só existe em produção)

## Sintoma
`testUniqueConstraintBlocksSameTime` falhava deterministicamente — `assertThrows(DataIntegrityViolationException)`
nunca disparava, mesmo salvando duas consultas com o mesmo profissional/horário. Confirmado que
o teste já falhava assim no `main` limpo, antes de qualquer mudança nesta correção — não era
regressão de nada recente, era quebrado desde que a V14 foi criada.

## Causa raiz
A trava real de "não pode haver duas consultas ativas no mesmo profissional/horário" é um
**índice único parcial** do Postgres (`V14__fix_appointment_unique_constraint.sql`):
```sql
CREATE UNIQUE INDEX idx_appointments_prof_date_active
ON appointments (professional_id, date_time) WHERE status != 'CANCELADA';
```
Isso não é expressável via `@UniqueConstraint` do JPA (que não suporta condição `WHERE`), então
a entidade `Appointment` não carrega essa restrição. Em teste, `spring.flyway.enabled=false` e o
schema vem só de `ddl-auto=create-drop` — ou seja, migrations SQL (incluindo a V14) nunca rodam
no banco de teste. O índice que o teste tenta provocar simplesmente não existe lá.

Agravante: H2 (o banco de teste) nem suporta a sintaxe `CREATE UNIQUE INDEX ... WHERE ...`
("índice parcial" é um recurso específico do Postgres) — então nem dava pra recriar a mesma
migration literalmente via `@Sql` no teste.

## Solução
`@Sql(executionPhase = BEFORE_TEST_METHOD)` na classe do teste, recriando a mesma semântica só
no H2 de teste com uma **coluna computada + índice único** (índice único ignora múltiplos `NULL`
tanto em H2 quanto em Postgres — é o padrão portável pra "unicidade condicional" quando o motor
não suporta índice parcial de verdade):
```sql
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS test_active_slot_key VARCHAR(100) AS
  (CASE WHEN status <> 'CANCELADA' THEN professional_id || '|' || date_time ELSE NULL END);
CREATE UNIQUE INDEX IF NOT EXISTS idx_appointments_test_active_slot ON appointments (test_active_slot_key);
```
Isso só existe no banco de teste (nunca toca produção/migrations) — o schema real continua
sendo só o índice parcial da V14.

**Regra geral:** qualquer invariante que só existe como migration SQL raw (não expressável em
anotação JPA) é invisível pros testes deste projeto, porque o Flyway está desligado em teste. Se
escrever um teste de integração pra uma trava desse tipo, ela precisa ser recriada explicitamente
no teste (via `@Sql`) — e H2 não fala a sintaxe de índice parcial do Postgres, então a recriação
não pode ser um copy-paste literal da migration.

## Ligado a
- [[expiracao-jwt-15min-virava-3h15min-em-producao]] — achado na mesma revisão minuciosa de
  todo o sistema (27/08/2026).
