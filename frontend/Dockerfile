# Estágio 1: Build
FROM node:18 AS build
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

# Estágio 2: Servidor
FROM nginx:alpine
# Copia o build para a pasta do Nginx
COPY --from=build /app/dist/seu-projeto-nome /usr/share/nginx/html
# Expõe a porta 80
EXPOSE 80
# O Nginx roda em foreground por padrão, mantendo o container vivo
CMD ["nginx", "-g", "daemon off;"]