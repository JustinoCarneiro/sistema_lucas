#!/bin/bash

# --- script de backup para o projeto lucas ---
# este script gera um dump do banco postgres local (homologação) e da produção
# e os mantém organizados, salvando também uma cópia da produção localmente.
# Desde 04/08/2026: dump criptografado (AES-256), cópia off-site no Google Drive
# via rclone e alerta por e-mail em caso de falha.

set -o pipefail

# Garante que os caminhos relativos (.env, ./backups) resolvem para o diretório
# do script, independente de onde ele for chamado (ex: cron não faz cd antes)
cd "$(dirname "$0")" || exit 1

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

KEY_FILE="./secrets/backup_encryption_key.txt"
MAIL_PASSWORD_FILE="./secrets/mail_password.txt"
RCLONE_REMOTE="gdrive:sistema-lucas-backups"

alert_falha() {
    local motivo="$1"
    if [ -f "$MAIL_PASSWORD_FILE" ] && [ -n "$MAIL_USERNAME" ] && [ -n "$INITIAL_ADMIN_EMAIL" ]; then
        local SMTP_PASS
        SMTP_PASS=$(cat "$MAIL_PASSWORD_FILE")
        {
            echo "To: $INITIAL_ADMIN_EMAIL"
            echo "From: $MAIL_USERNAME"
            echo "Subject: [Sistema Lucas] Falha no backup do banco - $TIMESTAMP"
            echo ""
            echo "O backup automatico do Sistema Lucas falhou em $TIMESTAMP."
            echo "Motivo: $motivo"
            echo "Verificar manualmente no servidor (/root/sistema/sistema_lucas/backup.sh)."
        } | curl -s --ssl-reqd --url "smtp://smtp.gmail.com:587" \
            --mail-from "$MAIL_USERNAME" --mail-rcpt "$INITIAL_ADMIN_EMAIL" \
            --user "$MAIL_USERNAME:$SMTP_PASS" --upload-file - || true
    fi
}

if [ ! -f "$KEY_FILE" ]; then
    echo "❌ Chave de criptografia $KEY_FILE não encontrada."
    alert_falha "Chave de criptografia ausente em $KEY_FILE"
    exit 1
fi

echo "==============================================="
echo "💾 Iniciando backup Local (Homologação)"
echo "==============================================="
LOCAL_FILE="$BACKUP_DIR/backup_homolog_$TIMESTAMP.sql.gz.enc"

if docker exec lucas-db pg_dump -U "$DB_USER" "$DB_NAME" \
    | gzip \
    | openssl enc -aes-256-cbc -pbkdf2 -salt -pass file:"$KEY_FILE" -out "$LOCAL_FILE"; then
    chmod 600 "$LOCAL_FILE"
    echo "✅ Backup local salvo: $LOCAL_FILE"
else
    echo "❌ Erro ao realizar backup local!"
    rm -f "$LOCAL_FILE"
    alert_falha "pg_dump/gzip/openssl retornou código de erro no backup local"
fi

echo ""
echo "==============================================="
echo "🌍 Iniciando backup Remoto (Produção)"
echo "==============================================="

# Se este script já está rodando dentro do próprio servidor de produção
# (ex: via cron em produção), pular a etapa de SSH: ela tentaria conectar
# na própria máquina e sempre falharia por falta de chave de loopback.
if hostname -I 2>/dev/null | grep -qw "$DEPLOY_SERVER_IP"; then
    echo "ℹ️  Rodando localmente no servidor de produção — pulando etapa de SSH remoto (o dump acima já é o de produção)."
else
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
fi

echo ""
echo "🧹 Limpando backups antigos locais (mais de 7 dias)..."
find "$BACKUP_DIR" -name "backup_*.sql.gz*" -mtime +7 -delete

# Cópia off-site (Google Drive via rclone) — só o arquivo já criptografado, nunca a chave
if [ -f "$LOCAL_FILE" ] && command -v rclone &> /dev/null; then
    if rclone copy "$LOCAL_FILE" "$RCLONE_REMOTE/" --retries 3 --low-level-retries 5; then
        echo "✅ Cópia off-site enviada para $RCLONE_REMOTE"
        rclone delete "$RCLONE_REMOTE" --min-age 7d 2>&1
    else
        echo "❌ Erro ao enviar cópia off-site para $RCLONE_REMOTE (backup local já está OK)."
        alert_falha "Backup local OK, mas envio off-site (rclone -> $RCLONE_REMOTE) falhou"
    fi
fi

echo "✨ Processo 100% finalizado!"
