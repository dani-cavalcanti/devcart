# syntax=docker/dockerfile:1

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# POM pai + POMs dos modulos (satisfaz a lista <modules> do reactor e cacheia dependencias)
COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY catalogo-service/pom.xml catalogo-service/pom.xml
COPY carrinho-service/pom.xml carrinho-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml
RUN mvn -q -pl discovery-server -DskipTests dependency:go-offline

COPY discovery-server/src discovery-server/src
RUN mvn -q -pl discovery-server -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/discovery-server/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
