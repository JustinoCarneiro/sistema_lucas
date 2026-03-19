#!/bin/sh

# Cria a pasta de assets se ela não existir
mkdir -p /usr/share/nginx/html/assets

# Cria/sobrescreve o env.js dinamicamente de acordo com o ambiente
cat <<EOF > /usr/share/nginx/html/assets/env.js
(function(window) {
  window.__env = window.__env || {};
  window.__env.apiUrl = "${API_URL:-http://localhost:8081}";
})(this);
EOF

# Inicia o Nginx fornecido originalmente no comando
exec nginx -g "daemon off;"
