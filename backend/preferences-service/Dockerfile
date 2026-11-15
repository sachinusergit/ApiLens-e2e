# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY preferences-service ./preferences-service
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY file-service/pom.xml ./file-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl preferences-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/preferences-service/target/preferences-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
