# Estágio de Build
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build --configuration=production

# Estágio de Runtime
FROM nginx:alpine
# Ajustado para o caminho padrão do Angular 17+
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY generate-env.sh /generate-env.sh
RUN chmod +x /generate-env.sh
EXPOSE 80
CMD ["/generate-env.sh"]