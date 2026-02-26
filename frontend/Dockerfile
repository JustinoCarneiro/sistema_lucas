# Estágio 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Estágio 2: Serve com Nginx
FROM nginx:alpine

# AJUSTE O CAMINHO ABAIXO:
# O Angular costuma gerar em dist/<nome-do-projeto>/browser
# De acordo com seu erro e package.json, tente este caminho:
COPY --from=build /app/dist/sistema-lucas-ui/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]