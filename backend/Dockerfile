FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Otimização de cache: baixa dependências primeiro
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Estágio Final
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]