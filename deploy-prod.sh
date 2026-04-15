#!/bin/bash

# --- script de deploy para PRODUÇÃO ---
# ATENÇÃO: Certifique-se de que o arquivo .env existe e está configurado com variáveis de produção.

if [ ! -f .env ]; then
    echo "❌ Erro: Arquivo .env não encontrado!"
    echo "Crie o arquivo baseado no template (ex. .env.example) antes de rodar este script."
    exit 1
fi

echo "🚀 Iniciando Deploy PRODUÇÃO (usando .env)..."

# No modo produção, passamos o perfil 'prod' para o Spring Boot
export SPRING_PROFILES_ACTIVE=prod

docker compose --env-file .env up -d --build --remove-orphans

echo "✅ Deploy de produção finalizado com sucesso Porto: 80"
echo "Monitorar logs com: docker compose logs -f"
