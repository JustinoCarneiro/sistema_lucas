#!/bin/bash
# Script utilitário para popular Docker Secrets locais
# Lê o .env e cria os arquivos txt na pasta secrets/

SECRETS_DIR="./secrets"

echo "🔐 Inicializando Docker Secrets Locais..."

# Cria a pasta se não existir
mkdir -p "$SECRETS_DIR"

# Função para extrair variável do .env e salvar num arquivo txt
# Ignora linhas comentadas
extract_secret() {
    local env_var="$1"
    local secret_file="$SECRETS_DIR/$2"
    
    # Extrai o valor (tudo depois do primeiro '=') ignorando comentários no .env
    local value=$(grep -v '^#' .env | grep "^${env_var}=" | cut -d '=' -f 2-)
    
    if [ ! -z "$value" ]; then
        # Remove aspas se existirem
        value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
        echo -n "$value" > "$secret_file"
        echo "✅ Secret gerado: $secret_file"
    else
        echo "⚠️  Aviso: Variável $env_var não encontrada no .env"
    fi
}

if [ ! -f .env ]; then
    echo "❌ Erro: Arquivo .env não encontrado na raiz."
    exit 1
fi

extract_secret "DB_PASS" "db_pass.txt"
extract_secret "JWT_SECRET" "jwt_secret.txt"
extract_secret "MAIL_PASSWORD" "mail_password.txt"
extract_secret "INITIAL_ADMIN_PASSWORD" "initial_admin_password.txt"
extract_secret "ENCRYPTION_KEY" "encryption_key.txt"
extract_secret "ENCRYPTION_KEY_OLD" "encryption_key_old.txt"

echo "🎉 Pronto! Secrets populados."
echo "IMPORTANTE: Não faça commit da pasta $SECRETS_DIR"
