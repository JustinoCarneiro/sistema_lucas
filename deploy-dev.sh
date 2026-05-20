#!/bin/bash

# --- script de deploy para desenvolvimento ---

echo "🚀 Iniciando Deploy DESENVOLVIMENTO (usando .env.dev)..."

export SPRING_PROFILES_ACTIVE=dev

# docker-compose.dev.yml troca o frontend para `ng serve` (hot-reload).
docker compose --env-file .env.dev \
  -f docker-compose.yml -f docker-compose.dev.yml \
  up -d --build --remove-orphans

echo "✅ Deploy de desenvolvimento finalizado com sucesso Porto: 8082 (hot-reload ativo)"
