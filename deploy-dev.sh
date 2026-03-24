#!/bin/bash

# --- script de deploy para desenvolvimento ---

echo "🚀 Iniciando Deploy DESENVOLVIMENTO (usando .env.dev)..."

docker compose --env-file .env.dev up -d --build --remove-orphans

echo "✅ Deploy de desenvolvimento finalizado com sucesso Porto: 8082"
