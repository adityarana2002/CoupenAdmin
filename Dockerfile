# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV ADMIN_USERNAME=""
ENV ADMIN_PASSWORD=""

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]
