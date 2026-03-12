FROM node:20-alpine AS build
WORKDIR /app

# 1. Copia apenas os arquivos de dependências
COPY package.json package-lock.json ./

# 2. Instala as dependências (Essa camada ficará guardada no seu PC)
RUN npm install --frozen-lockfile

# 3. Só DEPOIS copia o código (isso evita reinstalar tudo se você mudar só um .ts)
COPY . .
RUN npm run build