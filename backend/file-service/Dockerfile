# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY file-service ./file-service
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY preferences-service/pom.xml ./preferences-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl file-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/file-service/target/file-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
