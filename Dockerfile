FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar /app/tu-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/tu-backend.jar"]
