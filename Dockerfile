# syntax=docker/dockerfile:1
#
# Dockerfile parametrizado, reutilizado por todos os modulos Spring Boot.
# O modulo alvo e passado via build arg MODULE (ex.: MODULE=api-gateway).
#
ARG MODULE

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace

# POM pai + POMs de todos os modulos (satisfaz a lista <modules> do reactor)
COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY catalogo-service/pom.xml catalogo-service/pom.xml
COPY carrinho-service/pom.xml carrinho-service/pom.xml
COPY pedidos-service/pom.xml pedidos-service/pom.xml

COPY ${MODULE}/src ${MODULE}/src

# Cache do repositorio Maven entre builds (BuildKit)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${MODULE} -DskipTests package && \
    cp ${MODULE}/target/*.jar /workspace/app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
