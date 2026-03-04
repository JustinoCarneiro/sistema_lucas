# Estágio 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Copia arquivos de configuração de pacotes
COPY package*.json ./

# Instala as dependências (esta camada só muda se você alterar o package.json)
RUN npm install

# Copia o restante e gera o build
COPY . .
RUN npm run build

# Estágio 2: Serve com Nginx
FROM nginx:alpine
# Mantendo o caminho que você confirmou que funciona
COPY --from=build /app/dist/sistema-lucas-ui/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]