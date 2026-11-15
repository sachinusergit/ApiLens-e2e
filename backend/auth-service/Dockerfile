# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY auth-service ./auth-service
COPY file-service/pom.xml ./file-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY preferences-service/pom.xml ./preferences-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl auth-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/auth-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
