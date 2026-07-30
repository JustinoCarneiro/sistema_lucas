---
tipo: bug
data: 2026-07-15
severidade: Crítica
status: Resolvido
---

# Backup diário silenciosamente quebrado por ~2 meses (working directory do cron)

## Sintoma
Nenhum dump de banco era gerado havia cerca de 2 meses — falha completamente silenciosa, sem alerta, descoberta só numa auditoria.

## Causa raiz
O cron de produção rodava `backup.sh` a partir de um working directory diferente do diretório do script. O script carregava `.env` por caminho **relativo**, que nunca era encontrado nesse contexto — abortava antes de gerar qualquer dump, sem logar um erro visível o suficiente para chamar atenção.

## Solução
Script corrigido para resolver o caminho do `.env` de forma independente do working directory de quem o chama. Também passou a pular a etapa de SSH remoto quando já está rodando no próprio servidor de produção (evitava um erro inevitável de "Permission denied" ao tentar se conectar nele mesmo).

**Regra geral:** todo script chamado por cron precisa resolver caminhos relativos a partir do próprio diretório do script (`dirname "$0"` ou equivalente), nunca assumir o working directory de quem invoca. Falha de backup precisa de alerta ativo (e-mail/log monitorado), não só um exit code silencioso — considerar isso ao revisar `backup.sh` de novo.
