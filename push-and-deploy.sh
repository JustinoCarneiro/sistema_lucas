#!/bin/bash

# --- script para enviar e rodar o deploy no servidor ---

SERVER_IP="157.173.212.76"
SERVER_USER="root"
SERVER_PATH="~/sistema/sistema_lucas/"

echo "📡 Transferindo arquivos para o servidor ($SERVER_IP)..."

rsync -avz --exclude 'node_modules' --exclude 'target' --exclude 'dist' --exclude '.git' \
./ $SERVER_USER@$SERVER_IP:$SERVER_PATH

if [ $? -ne 0 ]; then
    echo "❌ Erro na transferência via RSYNC!"
    exit 1
fi

echo "🚀 Rodando o script de deploy no servidor remoto..."

ssh $SERVER_USER@$SERVER_IP "cd $SERVER_PATH && chmod +x deploy.sh && ./deploy.sh"

echo "✨ Processo concluído com sucesso!"
