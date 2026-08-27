#!/bin/bash

# --- script para enviar e rodar o deploy no servidor ---

# Carrega variáveis do .env (se existir)
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        if [[ -n "$key" && "$key" != \#* ]]; then
            value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//')
            export "$key=$value"
        fi
    done < .env
fi

SERVER_IP="${DEPLOY_SERVER_IP}"
SERVER_USER="${DEPLOY_SERVER_USER}"

if [ -z "$SERVER_IP" ] || [ -z "$SERVER_USER" ]; then
    echo "❌ Erro: DEPLOY_SERVER_IP ou DEPLOY_SERVER_USER não definidos no .env!"
    exit 1
fi

SERVER_PATH="~/sistema/sistema_lucas/"

echo "📡 Transferindo arquivos para o servidor ($SERVER_IP)..."

# --delete: sem isso, um arquivo removido/renomeado localmente nunca sumia do servidor sozinho —
# só ficava um arquivo novo ao lado do antigo, silenciosamente (foi a causa de dois incidentes de
# deploy em 27/08/2026, ver memoria-tecnica/bugs/deploy-crash-loop-v20-dado-legado-duplicado.md).
# --delete respeita os --exclude abaixo (não apaga o que está excluído da transferência), então
# secrets/, backups/ e .env nunca são tocados por isso.
#
# Antes de ativar --delete pela primeira vez, um dry-run (-n) revelou 4 documentos de compliance
# LGPD que só existiam no servidor, sem versionamento nenhum — resgatados pra docs/compliance/
# antes desta mudança (ver commit correspondente). Se um dia aparecer algo inesperado de novo no
# dry-run, resgatar antes de deixar o --delete apagar.
rsync -avz --delete \
--exclude 'node_modules' --exclude 'dist' --exclude '.git' --exclude 'target' \
--exclude 'backups' --exclude 'secrets' --exclude '.angular' \
--exclude 'cypress/downloads' --exclude 'cypress/videos' \
--exclude '.github-workflows-backup' --exclude 'testsprite_tests/tmp' \
./ $SERVER_USER@$SERVER_IP:$SERVER_PATH

if [ $? -ne 0 ]; then
    echo "❌ Erro na transferência via RSYNC!"
    exit 1
fi

echo "🚀 Rodando o script de deploy de PRODUÇÃO no servidor remoto..."

ssh $SERVER_USER@$SERVER_IP "cd $SERVER_PATH && chmod +x deploy-prod.sh && ./deploy-prod.sh && echo '--- LOGS DO BACKEND ---' && sleep 10 && docker logs lucas-api --tail 50"

echo "✨ Processo concluído com sucesso!"
