# Estágio 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# A mágica está aqui: Copia o pom e baixa as dependências primeiro
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Depois copia o código e compila (isso será muito rápido agora)
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]