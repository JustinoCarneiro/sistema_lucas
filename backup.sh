#!/bin/bash

# --- script de backup para o projeto lucas ---
# este script gera um dump do banco postgres e mantém os últimos 7 arquivos

BACKUP_DIR="./backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/backup_$TIMESTAMP.sql.gz"

# cria a pasta se não existir
mkdir -p "$BACKUP_DIR"

echo "💾 Iniciando backup do banco de dados..."

# executa o pg_dump dentro do container e compacta
docker exec lucas-db pg_dump -U postgres sistema_lucas | gzip > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Backup concluído: $BACKUP_FILE"
else
    echo "❌ Erro ao realizar backup!"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# remove backups com mais de 7 dias
echo "🧹 Limpando backups antigos (mais de 7 dias)..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +7 -delete

echo "✨ Processo finalizado!"
