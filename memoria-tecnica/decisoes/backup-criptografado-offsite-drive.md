---
tipo: decisao
data: 2026-08-04
status: Ativa
---

# Backup: dump passa a ser criptografado (AES-256) e ganha cópia off-site no Drive

## Contexto
Auditoria cruzada de backups em produção (mesmo servidor `157.173.212.76` hospeda Sistema Melvin, Sistema Lucas e SAW HUB) encontrou que o `backup.sh` do Lucas já rodava diariamente e com log corretos (a correção de [[backup-diario-quebrado-working-directory-cron]] segurou bem), mas:

- O dump ficava em texto puro (`.sql.gz`, só gzip, sem criptografia).
- Não existia cópia off-site — tudo (banco vivo + backups) morava no mesmo disco do mesmo servidor. Um problema de disco derrubaria os dois juntos.
- Os arquivos antigos de backup estavam com permissão `644` (qualquer usuário do servidor conseguia ler o dump do banco).
- A recomendação já deixada na nota do bug de working directory — "Falha de backup precisa de alerta ativo (e-mail/log monitorado), não só um exit code silencioso" — ainda não tinha sido implementada de fato (o log existia, mas nada *avisava* ativamente em caso de falha).

Essa auditoria começou no Sistema Melvin (repositório irmão, mesma infra), onde a mesma lacuna foi fechada primeiro; a solução foi só estendida pra cá reaproveitando a mesma infraestrutura de off-site já criada.

## Decisão
`backup.sh` alterado para:

- Dump passa por `gzip | openssl enc -aes-256-cbc -pbkdf2 -salt` antes de gravar (extensão virou `.sql.gz.enc`). Chave dedicada em `secrets/backup_encryption_key.txt` (`chmod 600`, **separada da chave de criptografia da aplicação** em `secrets/encryption_key.txt` — propositalmente, pra não acoplar rotação de uma com a outra).
- Cópia off-site enviada ao final de cada execução pra `gdrive:sistema-lucas-backups/` via `rclone` (mesmo remote/conta Google configurado no servidor pro Sistema Melvin — reaproveitado, não é uma conta exclusiva do Lucas).
- Alerta por e-mail em caso de falha (do dump local OU do envio off-site), via `curl --url smtp://smtp.gmail.com:587`, usando as credenciais **do próprio Lucas** (`MAIL_USERNAME` do `.env` + `secrets/mail_password.txt`), destinatário `INITIAL_ADMIN_EMAIL` — implementa finalmente a recomendação deixada em [[backup-diario-quebrado-working-directory-cron]].
- Retenção local mantida em 7 dias (não alterada); retenção no Drive espelhada em 7 dias também.
- `set -o pipefail` adicionado no topo — sem isso, o `if [ $? -eq 0 ]` de antes checava só o último estágio do pipe, não o `pg_dump` em si.
- Arquivos antigos em texto puro (`backup_homolog_*.sql.gz`, de antes desta mudança) tiveram a permissão apertada pra `600`.
- Testado ponta a ponta em produção: dump → cifra → local → off-site → limpeza, com round-trip de restauração confirmado (`openssl enc -d ... | gunzip | grep -c "PostgreSQL database dump complete"` → `1`).

### Comando de restauração
```bash
openssl enc -d -aes-256-cbc -pbkdf2 \
  -pass file:secrets/backup_encryption_key.txt \
  -in backups/backup_homolog_<timestamp>.sql.gz.enc \
| gunzip > restaurado.sql
```

## Consequências
- **A chave `secrets/backup_encryption_key.txt` é a única forma de ler os backups a partir de agora.** Está no `.gitignore` (pasta `secrets/` inteira já era ignorada) — não sobe pro Git. Existe uma cópia idêntica no servidor de produção (`/root/sistema/sistema_lucas/secrets/backup_encryption_key.txt`) e aqui no repo local; as duas precisam continuar iguais, senão um backup feito num ambiente não abre no outro. Se perder as duas cópias, os backups cifrados ficam permanentemente ilegíveis.
- **O remote `gdrive:` do rclone não está configurado neste repositório/máquina local** — só no servidor de produção. Se este script rodar localmente (fluxo "homologação" real, não como cron de produção) sem o rclone configurado aqui, a etapa de off-site é pulada silenciosamente se o `rclone` não estiver instalado, ou falha com alerta por e-mail se estiver instalado mas sem o remote `gdrive:` — não é um erro fatal pro backup local, só gera ruído. Configurar o rclone localmente (mesmo processo do servidor) se for rodar esse fluxo com frequência daqui.
- **Off-site depende de conta Google pessoal** (não institucional) — mesma ressalva já registrada no lado do Melvin.
- Antes de mexer nesse script de novo, ler esta nota e [[backup-diario-quebrado-working-directory-cron]] — a criptografia, o alerta por e-mail e o off-site são intencionais.

## Ligado a
- [[backup-diario-quebrado-working-directory-cron]]
