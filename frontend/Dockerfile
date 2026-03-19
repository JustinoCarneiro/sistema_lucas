# Estágio 1: Build
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration=production

# Estágio 2: Nginx
FROM nginx:alpine

# O caminho correto baseado no seu angular.json é dist/sistema-lucas/browser
# ou dist/sistema-lucas. Vamos copiar o conteúdo da pasta gerada:
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

COPY generate-env.sh /generate-env.sh
RUN chmod +x /generate-env.sh

# Configuração para evitar erro 404 no refresh (SPA)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["/bin/sh", "/generate-env.sh"]