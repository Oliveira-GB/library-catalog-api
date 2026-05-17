FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Library Catalog API"
LABEL description="Spring Boot Application - Library Catalog Management"

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
