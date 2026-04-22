#!/bin/bash

# --- script para enviar e rodar o deploy no servidor ---

# Carrega variáveis do .env (se existir)
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

SERVER_IP="${DEPLOY_SERVER_IP}"
SERVER_USER="${DEPLOY_SERVER_USER}"

if [ -z "$SERVER_IP" ] || [ -z "$SERVER_USER" ]; then
    echo "❌ Erro: DEPLOY_SERVER_IP ou DEPLOY_SERVER_USER não definidos no .env!"
    exit 1
fi

SERVER_PATH="~/sistema/sistema_lucas/"

echo "📡 Transferindo arquivos para o servidor ($SERVER_IP)..."

rsync -avz --exclude 'node_modules' --exclude 'target' --exclude 'dist' --exclude '.git' \
./ $SERVER_USER@$SERVER_IP:$SERVER_PATH

if [ $? -ne 0 ]; then
    echo "❌ Erro na transferência via RSYNC!"
    exit 1
fi

echo "🚀 Rodando o script de deploy de PRODUÇÃO no servidor remoto..."

ssh $SERVER_USER@$SERVER_IP "cd $SERVER_PATH && chmod +x deploy-prod.sh && ./deploy-prod.sh && echo '--- LOGS DO BACKEND ---' && sleep 10 && docker logs lucas-api --tail 50"

echo "✨ Processo concluído com sucesso!"
