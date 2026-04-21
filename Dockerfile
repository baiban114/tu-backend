FROM maven:3.9.11-eclipse-temurin-25 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /app/target/tu-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

