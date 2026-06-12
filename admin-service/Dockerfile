FROM maven:3.6-jdk-8 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY userfront-service/pom.xml userfront-service/
COPY admin-service/pom.xml admin-service/

RUN mvn dependency:go-offline -pl common,admin-service -am -B || true

COPY common/src common/src
COPY admin-service/src admin-service/src

RUN mvn clean package -pl common,admin-service -am -DskipTests -B

# --- Runtime stage ---
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/admin-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
