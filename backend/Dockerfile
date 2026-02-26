# Estágio 1: Build (Maven com Java 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para aproveitar o cache das camadas do Docker
COPY pom.xml .

# Em vez de go-offline, vamos rodar o package direto, 
# mas em passos separados para garantir a cópia do código
COPY src ./src
RUN mvn clean package -DskipTests -Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false

# Estágio 2: Runtime (JRE 21)
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]