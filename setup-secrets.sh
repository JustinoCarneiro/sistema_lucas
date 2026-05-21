#!/bin/bash
# Inicializa Docker Secrets locais (secrets/*.txt).
# Regras:
#   - Se o arquivo já existe e não está vazio → preserva (nunca sobrescreve).
#   - Secrets geráveis ausentes → gera valor aleatório forte (openssl rand -base64 48).
#   - Secrets manuais ausentes (db_pass, mail_password) → erro com instrução.
# Executar UMA VEZ na primeira configuração do servidor; depois só para adicionar novos secrets.

set -euo pipefail

SECRETS_DIR="./secrets"
mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"

ERROR=0

write_secret() {
    local file="$SECRETS_DIR/$1"
    local value="$2"
    if [ -s "$file" ]; then
        echo "  [ok] $1 — já existe, preservando."
    else
        printf '%s' "$value" > "$file"
        chmod 600 "$file"
        echo "  [gerado] $1"
    fi
}

require_manual() {
    local file="$SECRETS_DIR/$1"
    if [ -s "$file" ]; then
        echo "  [ok] $1 — já existe, preservando."
    else
        echo "  [ERRO] $1 não encontrado. Crie manualmente:"
        echo "         echo -n 'SEU_VALOR' > $file && chmod 600 $file"
        ERROR=1
    fi
}

generate() {
    openssl rand -base64 48 | tr -d '\n='
}

echo "Inicializando Docker Secrets em $SECRETS_DIR/ ..."

# --- Secrets manuais (valores fixos de infra) ---
require_manual "db_pass.txt"
require_manual "mail_password.txt"

# --- Secrets geráveis ---
write_secret "jwt_secret.txt"             "$(generate)"
write_secret "encryption_key.txt"         "$(generate)"
write_secret "cpf_hash_pepper.txt"        "$(generate)"
write_secret "initial_admin_password.txt" "$(generate)"

# --- Legacy keys (começa vazio; preencher durante rotação de chave) ---
if [ ! -f "$SECRETS_DIR/encryption_key_old.txt" ]; then
    touch "$SECRETS_DIR/encryption_key_old.txt"
    chmod 600 "$SECRETS_DIR/encryption_key_old.txt"
    echo "  [criado vazio] encryption_key_old.txt — preencher durante rotação de chave AES."
else
    echo "  [ok] encryption_key_old.txt — já existe, preservando."
fi

echo ""
if [ "$ERROR" -ne 0 ]; then
    echo "ATENÇÃO: Corrija os erros acima antes de iniciar os containers."
    exit 1
fi

echo "Secrets prontos. NUNCA faça commit da pasta $SECRETS_DIR/"
