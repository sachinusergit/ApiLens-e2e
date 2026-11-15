# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY gateway ./gateway
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY file-service/pom.xml ./file-service/pom.xml
COPY preferences-service/pom.xml ./preferences-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway/target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
