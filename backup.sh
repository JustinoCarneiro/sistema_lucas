#!/bin/bash

# --- script de backup para o projeto lucas ---
# este script gera um dump do banco postgres local (homologação) e da produção
# e os mantém organizados, salvando também uma cópia da produção localmente.

# Carrega as variáveis do .env de forma segura (sem executar eval de caracteres especiais)
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        if [[ -n "$key" && "$key" != \#* ]]; then
            # Remove aspas do valor se existirem
            value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//')
            export "$key=$value"
        fi
    done < .env
else
    echo "❌ Arquivo .env não encontrado. Crie-o para poder acessar a produção."
    exit 1
fi

BACKUP_DIR="./backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

echo "==============================================="
echo "💾 Iniciando backup Local (Homologação)"
echo "==============================================="
LOCAL_FILE="$BACKUP_DIR/backup_homolog_$TIMESTAMP.sql.gz"

docker exec lucas-db pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$LOCAL_FILE"
if [ $? -eq 0 ]; then
    echo "✅ Backup local salvo: $LOCAL_FILE"
else
    echo "❌ Erro ao realizar backup local!"
fi

echo ""
echo "==============================================="
echo "🌍 Iniciando backup Remoto (Produção)"
echo "==============================================="
PROD_FILE="backup_prod_$TIMESTAMP.sql.gz"
PROD_LOCAL_PATH="$BACKUP_DIR/$PROD_FILE"

# Diretório base no servidor de produção (onde costuma ficar o código)
REMOTE_PROJECT_DIR="~/sistema_lucas"
REMOTE_BACKUP_DIR="$REMOTE_PROJECT_DIR/backups"

echo "Conectando via SSH em $DEPLOY_SERVER_USER@$DEPLOY_SERVER_IP..."

# Comando que será executado na máquina de produção via SSH
REMOTE_CMD="mkdir -p $REMOTE_BACKUP_DIR && \
docker exec lucas-db pg_dump -U \"$DB_USER\" \"$DB_NAME\" | gzip > $REMOTE_BACKUP_DIR/$PROD_FILE && \
echo $REMOTE_BACKUP_DIR/$PROD_FILE && \
find $REMOTE_BACKUP_DIR -name 'backup_prod_*.sql.gz' -mtime +7 -delete"

# Executa o comando remoto e captura o caminho do arquivo gerado
REMOTE_PATH=$(ssh -o StrictHostKeyChecking=no $DEPLOY_SERVER_USER@$DEPLOY_SERVER_IP "$REMOTE_CMD")

if [ $? -eq 0 ] && [ -n "$REMOTE_PATH" ]; then
    # Pega apenas a última linha (o caminho do arquivo) caso o SSH retorne logs extras
    REMOTE_FILE=$(echo "$REMOTE_PATH" | tail -n 1)
    
    echo "✅ Backup remoto gerado na produção em: $REMOTE_FILE"
    echo "📥 Baixando cópia da produção para a sua máquina (homologação)..."
    
    scp -o StrictHostKeyChecking=no "$DEPLOY_SERVER_USER@$DEPLOY_SERVER_IP:$REMOTE_FILE" "$PROD_LOCAL_PATH"
    
    if [ $? -eq 0 ]; then
        echo "✅ Cópia do backup de produção salva com segurança em: $PROD_LOCAL_PATH"
    else
        echo "❌ Erro ao baixar o backup da produção."
    fi
else
    echo "❌ Erro de comunicação com o servidor de produção."
fi

echo ""
echo "🧹 Limpando backups antigos locais (mais de 7 dias)..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +7 -delete

echo "✨ Processo 100% finalizado!"
