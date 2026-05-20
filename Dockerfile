# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
LABEL maintainer="Library Catalog API"
LABEL description="Spring Boot Application - Library Catalog Management"

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/library-catalog-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
